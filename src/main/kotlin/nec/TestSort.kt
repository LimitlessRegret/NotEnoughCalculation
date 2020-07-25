package nec

import nec.dbmodel.DbRecipe
import nec.gui.calculation.GroupItemAmount
import nec.gui.calculation.RecipeGroup
import nec.solver.RecipeMPSolverWrapper
import org.jgrapht.Graph
import org.jgrapht.alg.scoring.PageRank
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.io.File
import java.util.*
import kotlin.collections.HashSet

object TestSort {
    private val recipeDb = RecipeDatabase()

    @JvmStatic
    fun main(args: Array<String>) {
        val rg = RecipeGroup()
        rg.load(File("test.rg.json"))
//        bfs(rg)
        test(rg)
    }

    private fun test(rg: RecipeGroup) {
        val graph = DefaultDirectedGraph<DbRecipe, DefaultEdge>(DefaultEdge::class.java)

        val recipesByProducts = rg.recipes.values
            .flatMap { recipe -> recipe.recipe.results.map { it.item?.id to recipe.recipe } }
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } }

        rg.recipes.values.forEach { graph.addVertex(it.recipe) }
        rg.recipes.values.forEach { sel ->
            sel.recipe.ingredients
                .mapNotNull { it.item?.id }
                .flatMap { recipesByProducts[it] ?: emptyList() }
                .toSet()
                .forEach {
                    graph.addEdge(it, sel.recipe)
                }
        }

        val pr = PageRank(graph)
        pr.scores.forEach {(recipe, score) ->
            println("${recipe.id}: $score")
        }
    }

    fun bfs(rg: RecipeGroup) {
        val recipesByProducts = rg.recipes.values
            .flatMap { recipe -> recipe.recipe.results.map { it.item?.id to recipe.recipeId } }
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } }
        val recipeIngredients = rg.recipes.values
            .associateWith { it.recipe.ingredients.mapNotNull { it.item?.id } }

        val seenRecipes = HashSet<Int>()
        val rootItems = rg.items.values
            .filter { it.wantAmount > 0 }
            .map { it.itemId }

        println("rootItems=$rootItems")
        val stack = Stack<Int>()
        stack.addAll(rootItems)

        while (stack.isNotEmpty()) {
            val top = stack.pop()
            val ingredients = recipeIngredients[top] ?: emptyList()
            val producingRecipes = ingredients
                .flatMap { recipesByProducts[it] ?: emptyList() }
                .toSet() - seenRecipes
            seenRecipes.addAll(producingRecipes)

        }
    }
}

