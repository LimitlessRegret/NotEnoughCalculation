package nec.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import nec.dbmodel.SqliteInterface
import nec.dbmodel.tables.pojos.Recipe
import nec.dbmodel.tables.pojos.RecipeItem
import nec.timeThis
import java.io.File

data class SchemaDbItem(
    var id: Int?,
    val localizedName: String,
    val internalName: String,
    val isFluid: Boolean,
    val damage: Int
)

class SchemaImporter {
    private val sqliteInterface = SqliteInterface("nec.db") // TODO
    private var recipeIdCounter = 1
    private val recipes = ArrayList<Recipe>()
    private val recipeItems = ArrayList<Array<Any?>>()

    fun loadFile(filename: String) {
        val jsonString = timeThis("load text") {
            File(filename).readText()
        }

        val model = timeThis("deserialize") {
            val json = Json(JsonConfiguration.Stable)
            json.parse(JsonDumpSchema.serializer(), jsonString)
        }

        load(model)
    }

    fun load(model: JsonDumpSchema) {
        sqliteInterface.saveItems(model.items.map { item ->
            SchemaDbItem(
                item.id,
                item.localizedName ?: "[null]",
                item.internalName ?: "[null]",
                item.isFluid,
                item.damage
            )
        })
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
        recipes.add(Recipe(recipeId, machine, recipe.en, recipe.duration.toInt(), recipe.eut?.toInt()))

        var slot = 0
        recipe.inputFluid.forEach {
            recipeItems.add(toRecipeItemArray(recipeId, it.id, null, slot++, it.amount, it.chance, false))
        }
        recipe.inputItems.forEach {
            recipeItems.add(toRecipeItemArray(recipeId, it.id, null, slot++, it.amount, it.chance, false))
        }

        slot = 0
        recipe.outputFluid.forEach {
            recipeItems.add(toRecipeItemArray(recipeId, it.id, null, slot++, it.amount, it.chance, true))
        }
        recipe.outputItems.forEach {
            recipeItems.add(toRecipeItemArray(recipeId, it.id, null, slot++, it.amount, it.chance, true))
        }
    }

    private fun loadRecipe(machine: String, recipe: SchemaRecipe) {
        val recipeId = recipeIdCounter++
        recipes.add(Recipe(recipeId, machine, true, null, null))

        (recipe.inputItems ?: listOf(recipe.inputItem!!))
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

        recipeItems.add(toRecipeItemArray(recipeId, recipe.output.id, null, 0, recipe.output.amount, null, true))
    }

    fun saveToDb() {
        timeThis("saveRecipes") {sqliteInterface.saveRecipes(recipes)}
        timeThis("saveRecipeItems") {sqliteInterface.saveRecipeItems(recipeItems)}
    }

    private inline fun toRecipeItemArray(
        recipeId: Int,
        itemId: Int?,
        oreDictId: Int?,
        slot: Int,
        amount: Int?,
        chance: Int?,
        isOutput: Boolean
    ):Array<Any?> = arrayOf(recipeId, itemId, oreDictId, slot, amount, chance, isOutput)
}
