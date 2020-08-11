package nec.model

import nec.dbmodel.SqliteInterface
import nec.dslJson
import nec.timeThis
import java.io.File

class SchemaImporter {
    private val sqliteInterface = SqliteInterface("nec.db") // TODO
    private var recipeIdCounter = 1
    private val recipes = ArrayList<Array<Any?>>()
    private val recipeItems = ArrayList<Array<Any?>>()
    private var modIdCounter = 1
    private val modMap = HashMap<String, Int>()

    fun loadFile(filename: String) {
        val model = timeThis("deserialize json") {
            dslJson.deserialize(
                JsonDumpSchema::class.java,
                File(filename).inputStream()
            )!!
        }

        load(model)
    }

    fun load(model: JsonDumpSchema) {
        sqliteInterface.saveItems(model.items.map { item ->
            arrayOf(
                item.id,
                item.localizedName ?: "[null]",
                item.internalName ?: "[null]",
                item.isFluid,
                item.damage,
                item.mod?.let { modMap.computeIfAbsent(it) { recipeIdCounter++ } }
            )
        })
        sqliteInterface.saveModNames(modMap.map { it.toPair() })
        sqliteInterface.saveOreDictNames(model.oreDict.mapIndexed { idx, oreDict -> oreDict.name to idx })
        sqliteInterface.saveOreDictItems(model.oreDict.flatMapIndexed { idx, oreDict ->
            oreDict.ids.map { idx to it }
        })
        model.sources.forEach {
            when {
                it.machines != null -> timeThis("load ${it.machines.size} ${it.type ?: it.name} machines") {
                    it.machines.forEach { machine ->
                        machine.recipes.forEach { loadRecipe(machine.name, it) }
                    }
                }
                it.recipes != null -> timeThis("load ${it.recipes.size} ${it.type ?: it.name} recipes") {
                    it.recipes.forEach { recipe ->
                        loadRecipe(it.type ?: it.name!!, recipe)
                    }
                }
                else -> TODO()
            }
        }
    }

    private fun loadRecipe(machine: String, recipe: SchemaMachineRecipe) {
        val recipeId = recipeIdCounter++
        recipes.add(toRecipeArray(recipeId, machine, recipe.en, recipe.duration.toInt(), recipe.eut?.toInt()))

        var slot = 0
        recipe.input
            .filterNotNull()
            .forEach {
                recipeItems.add(toRecipeItemArray(recipeId, it.id, null, slot++, it.amount, it.chance, false))
            }

        slot = 0
        recipe.output.forEach {
            recipeItems.add(toRecipeItemArray(recipeId, it.id, null, slot++, it.amount, it.chance, true))
        }
    }

    private fun loadRecipe(machine: String, recipe: SchemaRecipe) {
        val recipeId = recipeIdCounter++
        recipes.add(toRecipeArray(recipeId, machine, true, null, null))

        recipe.inputItems
            .withIndex()
            .filter { it.value != null }
            .forEach { (idx, item) ->
                if (item!!.oreDictIds != null) {
                    item.oreDictIds!!.forEach { oreDictId ->
                        recipeItems.add(toRecipeItemArray(recipeId, null, oreDictId, idx, item.amount, null, false))
                    }
                } else {
                    recipeItems.add(toRecipeItemArray(recipeId, item.id, null, idx, item.amount, null, false))
                }
            }

        recipe.output
            .withIndex()
            .forEach { (idx, item) ->
                recipeItems.add(toRecipeItemArray(recipeId, item.id, null, idx, item.amount, null, true))
            }
    }

    fun saveToDb() {
        timeThis("saveRecipes") { sqliteInterface.saveRecipes(recipes) }
        timeThis("saveRecipeItems") { sqliteInterface.saveRecipeItems(recipeItems) }
    }

    private inline fun toRecipeItemArray(
        recipeId: Int,
        itemId: Int?,
        oreDictId: Int?,
        slot: Int,
        amount: Int?,
        chance: Int?,
        isOutput: Boolean
    ): Array<Any?> = arrayOf(recipeId, itemId, oreDictId, slot, amount, chance, isOutput)

    private inline fun toRecipeArray(
        id: Int,
        source: String,
        isEnabled: Boolean,
        duration: Int?,
        euT: Int?
    ): Array<Any?> = arrayOf(id, source, isEnabled, duration, euT)
}
