package nec

import nec.model.SchemaImporter

object Test {
    //    private val recipeDump: RecipeDump by inject()
//    private val recipeDump = RecipeLoader()
    private val recipeDump = SchemaImporter()
    private val recipeDb = RecipeDatabase()

    @JvmStatic
    fun main(args: Array<String>) {
        recipeDump.loadFile("../NotEnoughProduction/lwjgl3/build/libs/cache/v2.0.9.0QF2-x0.0.3.json")
        timeThis("saveToDb") { recipeDump.saveToDb() }

        val ingredients =
            timeThis("Search by localized name") { recipeDb.lookupByLocalizedName("nitrogen plasma") }
        println(ingredients)
//        ingredients
//            .forEach { println(recipeDb.findLocalizedName(it)) }
        val recipes = timeThis("Lookup recipes by output") { recipeDb.findRecipeByResult(ingredients) }
        println("${recipes.size} recipes")
//        val recipeItems = timeThis("getRecipeItems") { recipeDb.sqliteInterface.getRecipeItems(recipes.map { it.id }) }
//            .map { it.itemId }
//        println("${recipeItems.size} recipe items")
//        val items = timeThis("getItems") { recipeDb.sqliteInterface.getItems(recipeItems) }
//        println("${items.size} items")

        recipes.forEach {
            println("recipe - ${it.id} - ${it.machine}")
            it.ingredients.forEach {
                println("    ingd: ${it.amount}x ${it.item}")
            }
            it.results.forEach {
                println("    rslt: ${it.amount}x ${it.item}")
            }
        }
        System.`in`.read()
    }
}