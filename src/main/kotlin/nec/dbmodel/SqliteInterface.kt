package nec.dbmodel

import nec.dbmodel.Tables.*
import nec.dbmodel.tables.pojos.Item
import nec.dbmodel.tables.pojos.Recipe
import nec.dbmodel.tables.pojos.RecipeItem
import nec.model.SchemaDbItem
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DataSourceConnectionProvider
import org.sqlite.SQLiteDataSource
import java.util.ArrayList

data class ItemDbKey(
    val internalName: String,
    val localizedName: String,
    val config: Int?
)

class SqliteInterface(database: String) {
    private val dslContext by lazy {
        System.getProperties().setProperty("org.jooq.no-logo", "true")

        DSL.using(DataSourceConnectionProvider(SQLiteDataSource().apply {
            url = "jdbc:sqlite:$database"
        }), SQLDialect.SQLITE)
    }

    fun saveItems(items: Map<ItemDbKey, Int>) = dslContext.transaction { txn ->
        DSL.using(txn).use { ctx ->
            val result = ctx
                .batchInsert(items.map {
                    ctx.newRecord(ITEM, it.key).apply {
                        id = it.value
                    }
                })
                .execute()
                .sum()
            println("saveItems(${items.size}) = $result")
        }
    }

    fun saveItems(items: Collection<SchemaDbItem>) = dslContext.transaction { txn ->
        DSL.using(txn).use { ctx ->
            val result = ctx
                .batchInsert(items.map { ctx.newRecord(ITEM, it) })
                .execute()
                .sum()
            println("saveItems(${items.size}) = $result")
        }
    }

    fun saveRecipes(recipeList: Collection<Recipe>) = dslContext.transaction { txn ->
        DSL.using(txn).use { ctx ->
            val result = ctx
                .batchInsert(recipeList.map { ctx.newRecord(RECIPE, it) })
                .execute()
                .sum()
            println("saveRecipes(${recipeList.size}) = $result")
        }
    }

    fun saveRecipeItems(recipeItemList: Collection<RecipeItem>) = dslContext.transaction { txn ->
        DSL.using(txn).use { ctx ->
            val result = ctx
                .batchInsert(recipeItemList.map { ctx.newRecord(RECIPE_ITEM, it) })
                .execute()
                .sum()
            println("saveRecipeItems(${recipeItemList.size}) = $result")
        }
    }

    fun searchItems(query: String) = dslContext.use {
        val leadingWild = !query.startsWith('"')
        val trailingWild = !query.endsWith('"')

        val sb = StringBuilder()
        if (leadingWild) sb.append('%')
        sb.append(query.trim('"').replace('*', '%'))
        if (trailingWild) sb.append('%')

        println("query: ${sb.toString()}")

        it
            .select(ITEM.ID)
            .from(ITEM)
            .where(ITEM.LOCALIZED_NAME.like(sb.toString()))
//            .or(ITEM.INTERNAL_NAME.like("%$query%"))
            .fetch { it.component1() }
    }

    fun getRecipeItems(recipeIds: Collection<Int>) = dslContext.use {
        it
            .selectFrom(RECIPE_ITEM)
            .where(RECIPE_ITEM.RECIPE_ID.`in`(recipeIds))
            .fetchInto(RecipeItem::class.java)
    }

    fun getItems(itemIds: Collection<Int>) = dslContext.use {
        it
            .selectFrom(ITEM)
            .where(ITEM.ID.`in`(itemIds))
            .fetchInto(Item::class.java)
    }

    fun findRecipeByItem(itemIds: Collection<Int>, isOutput: Boolean? = null) = dslContext.use {
        it
            .selectDistinct(RECIPE.asterisk())
            .from(RECIPE_ITEM)
            .innerJoin(RECIPE)
            .on(RECIPE_ITEM.RECIPE_ID.eq(RECIPE.ID))
            .where(RECIPE_ITEM.ITEM_ID.`in`(itemIds))
            .and(
                when (isOutput) {
                    true -> RECIPE_ITEM.IS_OUTPUT.isTrue
                    false -> RECIPE_ITEM.IS_OUTPUT.isFalse
                    else -> DSL.noCondition()
                }
            )
            .and(RECIPE.IS_ENABLED.isTrue)
            .fetchInto(Recipe::class.java)
    }

    fun getRecipe(recipeId: Int): Recipe = dslContext.use {
        it
            .selectFrom(RECIPE)
            .where(RECIPE.ID.eq(recipeId))
            .fetchOneInto(Recipe::class.java)
    }
}