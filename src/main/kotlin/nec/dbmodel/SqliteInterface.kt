package nec.dbmodel

import nec.DbOreDictInfo
import nec.dbmodel.Tables.*
import nec.dbmodel.tables.pojos.Item
import nec.dbmodel.tables.pojos.Recipe
import nec.dbmodel.tables.pojos.RecipeItem
import nec.timeThis
import org.jooq.Record
import org.jooq.SQLDialect
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.DataSourceConnectionProvider
import org.sqlite.SQLiteDataSource
import java.sql.Connection

class SqliteInterface(database: String) {
    private val dslContext by lazy {
        System.getProperties().setProperty("org.jooq.no-logo", "true")

        DSL.using(DataSourceConnectionProvider(SQLiteDataSource().apply {
            url = "jdbc:sqlite:$database"
        }), SQLDialect.SQLITE)
    }

    fun saveItems(items: Collection<Array<Any?>>) =
        bulkInsertRaw("saveItems", ITEM, items)

    fun saveOreDictNames(oreDicts: Collection<Pair<String, Int>>) =
        bulkInsertRaw("saveOreDictNames", ORE_DICT_GROUP, oreDicts
            .map { arrayOf(it.second, it.first) })

    fun saveModNames(mods: Collection<Pair<String, Int>>) =
        bulkInsertRaw("saveModNames", MOD, mods
            .map { arrayOf(it.second, it.first) })

    fun saveOreDictItems(oreDictItems: Collection<Pair<Int, Int>>) =
        bulkInsertRaw("saveOreDictItems", ORE_DICT_ITEM, oreDictItems
            .map { arrayOf(it.first, it.second) })

    fun saveRecipes(recipeList: Collection<Array<Any?>>) =
        bulkInsertRaw("saveRecipes", RECIPE, recipeList)

    fun saveRecipeItems(recipeItemList: Collection<Array<Any?>>) =
        bulkInsertRaw("saveRecipeItems", RECIPE_ITEM, recipeItemList)

    fun saveMetadata(key: String, value: String) = dslContext.use {
        it.insertQuery(METADATA).apply {
            addRecord(it.newRecord(METADATA)
                .apply {
                    this.key = key
                    this.value = value
                })
            onDuplicateKeyUpdate(true)
            addValueForUpdate(METADATA.VALUE, value)
            execute()
        }
    }

    private fun withSqlTransaction(block: (conn: Connection) -> Unit) {
        dslContext.connection { conn ->
            val txStmnt = conn.createStatement()
            txStmnt.execute("PRAGMA journal_mode = OFF;")
            txStmnt.execute("PRAGMA locking_mode = EXCLUSIVE;")
            txStmnt.execute("PRAGMA synchronous = OFF;")

            txStmnt.execute("BEGIN TRANSACTION;")
            block(conn)
            txStmnt.execute("COMMIT TRANSACTION;")

            txStmnt.close()
        }
    }

    private fun <T : Table<R>, R : Record> bulkInsertRaw(
        logTag: String,
        table: T,
        items: Collection<Array<Any?>>
    ) = withSqlTransaction { conn ->
        val fakeRecord = dslContext.newRecord(table).apply { changed(true) }
        val fields = fakeRecord.fields()
            .map { it.dataType.sqlDataType.sqlType }
            .toIntArray()
        val rawSql = dslContext.insertQuery(table).use { q ->
            q.addRecord(fakeRecord)
            q.sql
        }

        val stmt = conn.prepareStatement(rawSql)
        timeThis("add batches", debug = true) {
            items.forEach { item ->
                fields.forEachIndexed { idx, field ->
                    val value = item[idx]
                    if (value == null) {
                        stmt.setNull(idx + 1, field)
                    } else {
                        stmt.setObject(idx + 1, value, field)
                    }
                }
                stmt.addBatch()
            }
        }

        val result = timeThis("executeBatch", debug = true) { stmt.executeBatch() }
        println("$logTag(${items.size}) = ${result.sum()}")
    }

    fun searchItems(query: String, itemIds: Set<Int>? = null) = dslContext.use {
        val leadingWild = !query.startsWith('"')
        val trailingWild = !query.endsWith('"')

        val sb = StringBuilder()
        if (leadingWild) sb.append('%')
        sb.append(query.trim('"').replace('*', '%'))
        if (trailingWild) sb.append('%')

        it
            .select(ITEM.ID)
            .from(ITEM)
            .where(itemIds?.let { ITEM.ID.`in`(it) } ?: DSL.noCondition())
            .and(ITEM.LOCALIZED_NAME.like(sb.toString()))
//            .or(ITEM.INTERNAL_NAME.like("%$query%"))
            .fetch { it.component1() }
    }

    fun getAllMods() = dslContext.use {
        it
            .selectFrom(MOD)
            .associate { it.component1() to it.component2() }
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

    fun getOreDicts(ids: Collection<Int>) = dslContext.use {
        val groupItems = it
            .selectFrom(ORE_DICT_ITEM)
            .where(ORE_DICT_ITEM.ORE_DICT_ID.`in`(ids))
            .groupBy { it.oreDictId }
            .mapValues { it.value.map { it.itemId }.toIntArray() }

        it
            .selectFrom(ORE_DICT_GROUP)
            .where(ORE_DICT_GROUP.ID.`in`(ids))
            .fetch { DbOreDictInfo(it.id, it.name, groupItems[it.id] ?: intArrayOf()) }
    }

    fun findRecipeByItem(
        itemIds: Collection<Int>,
        oreDictIds: Collection<Int>,
        isOutput: Boolean? = null
    ) = dslContext.use {
        it
            .selectDistinct(RECIPE.asterisk())
            .from(RECIPE_ITEM)
            .innerJoin(RECIPE)
            .on(RECIPE_ITEM.RECIPE_ID.eq(RECIPE.ID))
            .where(
                RECIPE_ITEM.ITEM_ID.`in`(itemIds)
                    .or(RECIPE_ITEM.ORE_DICT_ID.`in`(oreDictIds))
            )
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

    fun getOreDictsFor(itemIds: Collection<Int>): List<Int> = dslContext.use {
        it
            .select(ORE_DICT_ITEM.ORE_DICT_ID)
            .from(ORE_DICT_ITEM)
            .where(ORE_DICT_ITEM.ITEM_ID.`in`(itemIds))
            .fetch { it.component1() }
    }
}