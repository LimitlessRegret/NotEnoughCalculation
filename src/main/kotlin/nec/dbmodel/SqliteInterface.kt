package nec.dbmodel

import nec.dbmodel.Tables.*
import nec.dbmodel.tables.pojos.*
import nec.model.SchemaDbItem
import nec.timeThis
import org.jooq.Record
import org.jooq.SQLDialect
import org.jooq.Table
import org.jooq.TransactionalRunnable
import org.jooq.impl.DSL
import org.jooq.impl.DataSourceConnectionProvider
import org.sqlite.SQLiteDataSource

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

    fun saveItems(items: Collection<SchemaDbItem>) =
        bulkInsert("saveItems", ITEM, items)

    fun saveOreDictNames(oreDicts: Collection<Pair<String, Int>>) =
        bulkInsert("saveOreDictNames", ORE_DICT_GROUP, oreDicts
            .map { OreDictGroup(it.second, it.first) })

    fun saveOreDictItems(oreDictItems: Collection<Pair<Int, Int>>) =
        bulkInsert("saveOreDictItems", ORE_DICT_ITEM, oreDictItems
            .map { OreDictItem(it.first, it.second) })

    fun saveRecipes(recipeList: Collection<Recipe>) =
        bulkInsert("saveRecipes", RECIPE, recipeList)

    fun saveRecipeItems(recipeItemList: Collection<Array<Any?>>) =
        bulkInsertRaw("saveRecipeItems", RECIPE_ITEM, recipeItemList)

    private fun <T : Table<R>, R : Record, S : Any> bulkInsert(
        logTag: String,
        table: T,
        items: Collection<S>
    ) = dslContext.use { ctx ->
        ctx.execute("PRAGMA journal_mode = OFF;")
        ctx.execute("PRAGMA locking_mode = EXCLUSIVE;")
        ctx.execute("PRAGMA synchronous = OFF;")

        ctx.transaction(TransactionalRunnable {
            DSL.using(it).use { ctx ->
                val records = timeThis("Prepare records") { items.map { ctx.newRecord(table, it) } }

                val result = ctx.insertInto(table)
                    .columns(*records.first().fields())
                    .let { query ->
                        timeThis("array map") {
                            records.map { query.values(*it.intoArray()) }
                                .last()
                        }
                    }
                    .execute()

                println("$logTag(${items.size}) = $result")
            }
        })
    }

    private fun <T : Table<R>, R : Record> bulkInsertRaw(
        logTag: String,
        table: T,
        items: Collection<Array<Any?>>
    ) = dslContext.use { ctx ->
        ctx.execute("PRAGMA journal_mode = OFF;")
        ctx.execute("PRAGMA locking_mode = EXCLUSIVE;")
        ctx.execute("PRAGMA synchronous = OFF;")

        ctx.transaction(TransactionalRunnable {
            DSL.using(it).use { ctx ->
                val result = ctx.insertInto(table)
                    .columns(*ctx.newRecord(table).fields())
                    .let { query ->
                        timeThis("array map") {
                            items.map { query.values(*it) }
                                .last()
                        }
                    }
                    .execute()

                println("$logTag(${items.size}) = $result")
            }
        })
    }

    fun searchItems(query: String) = dslContext.use {
        val leadingWild = !query.startsWith('"')
        val trailingWild = !query.endsWith('"')

        val sb = StringBuilder()
        if (leadingWild) sb.append('%')
        sb.append(query.trim('"').replace('*', '%'))
        if (trailingWild) sb.append('%')

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