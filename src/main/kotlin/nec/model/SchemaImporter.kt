package nec.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import nec.dbmodel.DbRecipe
import nec.dbmodel.SqliteInterface
import nec.dbmodel.tables.pojos.Item
import nec.dbmodel.tables.pojos.Recipe
import nec.dbmodel.tables.pojos.RecipeItem
import nec.timeThis
import java.io.File

data class SchemaDbItem(
    var id: Int?,
    val localizedName: String,
    val internalName: String,
    val isFluid: Boolean,
    val config: Int?
)

class SchemaImporter {
    private val sqliteInterface = SqliteInterface("nec.db") // TODO
    private var recipeIdCounter = 1
    private var itemIdCounter = 1
    private val itemCache = HashMap<SchemaDbItem, Int>()
    private val recipes = ArrayList<Recipe>()
    private val recipeItems = ArrayList<RecipeItem>()

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
        model.sources.forEach {
            when {
                it.machines != null -> timeThis("load ${it.machines.size} ${it.type} machines") {
                    it.machines.forEach { machine ->
                        machine.recipes.forEach { loadRecipe(machine.name, it) }
                    }
                }
                it.recipes != null -> timeThis("load ${it.recipes.size} ${it.type} recipes") {
                    it.recipes.forEach { recipe ->
                        loadRecipe(it.type, recipe)
                    }
                }
                else -> TODO()
            }
        }
    }

    private fun loadItem(item: SchemaItem, fluid: Boolean): Pair<Int, Int> {
        if (item.internalName == null) {
            return -1 to item.amount.toInt()
        }

        val dbItem = SchemaDbItem(null, item.localizedName!!, item.internalName, fluid, item.cfg?.toInt())
        val itemId = itemCache.computeIfAbsent(dbItem) { itemIdCounter++ }

        return itemId to item.amount.toInt()
    }

    private fun loadItems(item: SchemaCTItem): Collection<Pair<Int, Int?>> {
        if (item.internalName != null) {
            val dbItem = SchemaDbItem(null, item.localizedName!!, item.internalName, false, null)
            val itemId = itemCache.computeIfAbsent(dbItem) { itemIdCounter++ }

            return listOf(itemId to item.amount!!.toInt())
        }

        return item.itemsMatching
            ?.map { loadItem(it, false) }
            ?: emptyList()
    }

    private fun loadRecipe(machine: String, recipe: SchemaMachineRecipe) {
        val recipeId = recipeIdCounter++
        recipes.add(Recipe(recipeId, machine, recipe.en, recipe.duration.toInt(), recipe.eut.toInt()))

        var slot = 0
        recipe.inputFluid.forEach {
            val (itemId, itemAmt) = loadItem(it, true)
            recipeItems.add(RecipeItem(recipeId, itemId, slot++, itemAmt, false))
        }
        recipe.inputItems.forEach {
            val (itemId, itemAmt) = loadItem(it, false)
            recipeItems.add(RecipeItem(recipeId, itemId, slot++, itemAmt, false))
        }

        slot = 0
        recipe.outputFluid.forEach {
            val (itemId, itemAmt) = loadItem(it, true)
            recipeItems.add(RecipeItem(recipeId, itemId, slot++, itemAmt, true))
        }
        recipe.outputItems.forEach {
            val (itemId, itemAmt) = loadItem(it, false)
            recipeItems.add(RecipeItem(recipeId, itemId, slot++, itemAmt, true))
        }
    }

    private fun loadRecipe(machine: String, recipe: SchemaRecipe) {
        val recipeId = recipeIdCounter++
        recipes.add(Recipe(recipeId, machine, true, null, null))

        recipe.inputItems
            .withIndex()
            .filter { it.value != null }
            .forEach { (idx, item) ->
                loadItems(item!!).forEach { (itemId, itemAmt) ->
                    recipeItems.add(RecipeItem(recipeId, itemId, idx, itemAmt, false))
                }
            }

        val (itemId, itemAmt) = loadItem(recipe.output, false)
        recipeItems.add(RecipeItem(recipeId, itemId, 0, itemAmt, true))
    }

    fun saveToDb() {
        // apply item id to item pojos
        itemCache.forEach { (item, id) ->
            item.id = id
        }

        sqliteInterface.saveItems(itemCache.keys)
        sqliteInterface.saveRecipes(recipes)
        sqliteInterface.saveRecipeItems(recipeItems)
    }

}
