package nec

import nec.dbmodel.DbRecipe
import nec.dbmodel.SqliteInterface
import nec.dbmodel.tables.pojos.Item
import nec.dbmodel.tables.pojos.Recipe
import nec.dbmodel.tables.pojos.RecipeItem
import org.slf4j.LoggerFactory

data class DbOreDictInfo(
    val id: Int,
    val name: String,
    val itemsIds: IntArray,
) {
    lateinit var items: Array<Item>
}

data class RecipeOreDictItemAmount(
    val oreDicts: Collection<DbOreDictInfo>,
    val amount: Int,
)

data class RecipeItemAmount(
    val item: Item,
    val amount: Int,
    val chance: Int?
)

typealias OreDictSet = Collection<Int>

class RecipeDatabase {
    companion object {
        val instance = RecipeDatabase()
        private val LOG = LoggerFactory.getLogger(RecipeDatabase::class.java)
    }

    private val sqliteInterface = SqliteInterface("nec.db") // TODO
    private val itemCache = HashMap<Int, Item>()
    private val recipeCache = HashMap<Int, DbRecipe>()
    private val oreDictCache = HashMap<Int, DbOreDictInfo>()

    fun lookupByLocalizedName(query: String): Collection<Int> {
        return sqliteInterface.searchItems(query)
    }

    fun findRecipeByIngredient(itemId: Int) =
        findRecipeByIngredient(listOf(itemId))

    fun findRecipeByIngredient(itemIds: Collection<Int>) =
        findRecipeByItem(itemIds, sqliteInterface.getOreDictsFor(itemIds), isOutput = false)

    fun findRecipeByResult(itemId: Int) =
        findRecipeByResult(listOf(itemId))

    fun findRecipeByResult(itemIds: Collection<Int>) =
        findRecipeByItem(itemIds, sqliteInterface.getOreDictsFor(itemIds), isOutput = true)

    fun getRecipe(recipeId: Int): DbRecipe {
        return recipeCache[recipeId]
            ?: loadRecipes(listOf(sqliteInterface.getRecipe(recipeId)))
                .first()
    }

    private fun findRecipeByItem(
        itemIds: Collection<Int>,
        oreDictIds: Collection<Int>,
        isOutput: Boolean?
    ): Collection<DbRecipe> {
        val recipes = sqliteInterface.findRecipeByItem(itemIds, oreDictIds, isOutput)

        return loadRecipes(recipes)
    }

    private fun loadRecipes(recipes: Collection<Recipe>): Collection<DbRecipe> {
        val (cacheHit, cacheMiss) = recipes.partition { it.id in recipeCache }
        if (cacheMiss.isEmpty()) {
            return cacheHit.map { recipeCache.getValue(it.id) }
        }

        val recipeItems = sqliteInterface.getRecipeItems(cacheMiss.map { it.id })
        val referencedOreDicts = recipeItems
            .mapNotNull { it.oreDictId }
            .toSet()
        val recipeItemsByRecipe = recipeItems.groupBy { it.recipeId }

        ensureOreDictsLoaded(referencedOreDicts)
        ensureItemsLoaded(recipeItems.mapNotNull { it.itemId } +
                referencedOreDicts.flatMap { oreDictCache[it]?.itemsIds?.toList() ?: emptyList() })
        referencedOreDicts
            .mapNotNull { oreDictCache[it] }
            .forEach {
                it.items = it.itemsIds
                    .map { itemCache[it] }
                    .filterNotNull()
                    .toTypedArray()
            }

        val missed = cacheMiss.map { recipe ->
            prepareRecipe(recipe, recipeItemsByRecipe[recipe.id] ?: emptyList())
        }
        missed.associateByTo(recipeCache) { it.id }

        return cacheHit.map { recipeCache.getValue(it.id) } + missed
    }

    private fun prepareRecipe(
        recipe: Recipe,
        recipeItems: Collection<RecipeItem>
    ): DbRecipe {
        val (output, input) = recipeItems.partition { it.isOutput }
        val (oreDictSlots, nonOreDictSlots) = input.partition { it.oreDictId != null }

        // sanity check - there should be only one possible item in non-oredict slots
        nonOreDictSlots
            .groupBy { it.slot }
            .forEach { (_, v) ->
                require(v.size == 1)
            }

        val oreDictInputs = oreDictSlots
            .groupBy { it.slot }
            .mapValues {
                @Suppress("USELESS_CAST")
                it.value.mapNotNull { it.oreDictId } as OreDictSet to it.value.first().amount
            }
            .toList()
            .groupBy { it.second.first } // group by ore dicts allowed in slot
            .mapValues { (_, v) ->
                // combine amounts for slots with the same sets of ore dicts
                v.sumBy { it.second.second } to v.map { it.first }
            }
            .toList()
            .sortedBy {
                it.second.second.minOrNull()
            }
            .map { (oreDicts, info) ->
                // discard slot info
                RecipeOreDictItemAmount(oreDicts.map { oreDictCache.getValue(it) }, info.first)
            }

        return DbRecipe(
            recipe,
            input
                .filter { it.itemId in itemCache }
                .groupBy { it.itemId to it.chance }
                .map {
                    RecipeItemAmount(
                        itemCache.getValue(it.key.first),
                        it.value.sumBy { it.amount ?: 0 },
                        it.key.second
                    )
                },
            oreDictInputs,
            output
                .filter { it.itemId in itemCache }
                .map { RecipeItemAmount(itemCache.getValue(it.itemId), it.amount ?: 0, it.chance) },
        )
    }

    fun ensureOreDictsLoaded(ids: Collection<Int>) {
        val itemIds = ids.toSet()
        val (cacheHit, cacheMiss) = itemIds.partition { it in oreDictCache }
        LOG.debug("ensureOreDictsLoaded(${itemIds.size} items) - hit: $cacheHit, miss: $cacheMiss")

        if (cacheMiss.isNotEmpty()) {
            timeThis("load ${cacheMiss.size} ore dict groups into cache", debug = true) {
                sqliteInterface.getOreDicts(cacheMiss).associateByTo(oreDictCache) { it.id }
            }
        }
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
}
