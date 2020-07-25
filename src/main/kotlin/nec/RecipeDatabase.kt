package nec

import nec.dbmodel.DbRecipe
import nec.dbmodel.DbRecipeItem
import nec.dbmodel.SqliteInterface
import nec.dbmodel.tables.pojos.Item
import nec.dbmodel.tables.pojos.Recipe
import org.slf4j.LoggerFactory

class RecipeDatabase {
    companion object {
        val instance = RecipeDatabase()
        private val LOG = LoggerFactory.getLogger(RecipeDatabase::class.java)
    }

    private val sqliteInterface = SqliteInterface("nec.db") // TODO
    private val itemCache = HashMap<Int, Item>()
    private val recipeCache = HashMap<Int, DbRecipe>()

    fun lookupByLocalizedName(query: String): Collection<Int> {
        return sqliteInterface.searchItems(query)
    }

    fun findRecipeByIngredient(itemId: Int) =
        findRecipeByIngredient(listOf(itemId))

    fun findRecipeByIngredient(itemIds: Collection<Int>) =
        findRecipeByItem(itemIds, isOutput = false)

    fun findRecipeByResult(itemId: Int) =
        findRecipeByResult(listOf(itemId))

    fun findRecipeByResult(itemIds: Collection<Int>) =
        findRecipeByItem(itemIds, isOutput = true)

    fun getRecipe(recipeId: Int): DbRecipe {
        return recipeCache[recipeId]
            ?: loadRecipes(listOf(sqliteInterface.getRecipe(recipeId)))
                .first()
    }

    private fun findRecipeByItem(itemIds: Collection<Int>, isOutput: Boolean?): Collection<DbRecipe> {
        val recipes = sqliteInterface.findRecipeByItem(itemIds, isOutput)

        return loadRecipes(recipes)
    }

    private fun loadRecipes(recipes: Collection<Recipe>): Collection<DbRecipe> {
        val (cacheHit, cacheMiss) = recipes.partition { it.id in recipeCache }
        if (cacheMiss.isEmpty()) {
            return cacheHit.map { recipeCache.getValue(it.id) }
        }

        val recipeItems = sqliteInterface.getRecipeItems(cacheMiss.map { it.id })
            .groupBy { Triple(it.recipeId, it.slot, it.isOutput) }
            .map { (key, value) ->
                value
                    .sortedBy { it.itemId }
                    .let {
                        if (key.second == null) it
                        else it.take(1)
                    }
            }
            .flatten()
        val recipeItemsByRecipe = recipeItems.groupBy { it.recipeId }

        ensureItemsLoaded(recipeItems.map { it.itemId })

        val missed = cacheMiss.map { recipe ->
            DbRecipe(
                recipe,
                recipeItemsByRecipe[recipe.id]
                    ?.map { DbRecipeItem(it, itemCache[it.itemId]) }
                    ?: emptyList()
            )
        }
        missed.associateByTo(recipeCache) { it.id }

        return cacheHit.map { recipeCache.getValue(it.id) } + missed
    }

    private fun ensureItemsLoaded(itemIds: List<Int>) {
        val itemIds = itemIds.toSet()
        val (cacheHit, cacheMiss) = itemIds.partition { it in itemCache }
        LOG.debug("ensureItemsLoaded(${itemIds.size} items) - hit: $cacheHit, miss: $cacheMiss")

        if (cacheMiss.isNotEmpty()) {
            timeThis("load ${cacheMiss.size} items into cache", debug = true) {
                sqliteInterface.getItems(cacheMiss).associateByTo(itemCache) { it.id }
            }
        }
    }

    fun getItem(itemId: Int): Item {
        ensureItemsLoaded(listOf(itemId))

        return itemCache.getValue(itemId)
    }

//    fun findLocalizedName(internalName: String): String {
//        return itemNameMap.getOrDefault(internalName, "[$internalName]")
//    }

//    fun lookupRecipeMachine(recipe: RecipeOrMachineRecipe): String {
//        return recipeSourceMap.getOrDefault(recipe, "Unknown")
//    }
}
