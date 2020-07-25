package nec

import nec.gui.calculation.GroupItemAmount
import nec.gui.calculation.RecipeGroup
import nec.solver.RecipeMPSolverWrapper
import java.io.File

object TestCalculate {
    private val recipeDb = RecipeDatabase()

    @JvmStatic
    fun main(args: Array<String>) {
        val targetItemId = 11274

        val rmsw = RecipeMPSolverWrapper()
        rmsw.addRecipe(recipeDb.getRecipe(193341)) // pyrolyse oven in ct
        rmsw.addRecipe(recipeDb.getRecipe(125580)) // iron plate in pb

        val rg = RecipeGroup()
        rg.addRecipe(193341)
        rg.addRecipe(125580)
        rg.items[targetItemId] = GroupItemAmount(targetItemId).apply { wantAmount=1.0 }

        rmsw.applyItemConfiguration(GroupItemAmount(targetItemId).apply { wantAmount = 1.0 })
        val results = timeThis("getResults") { rmsw.getResults() }
        rmsw.printState()
        println("results.grossResults=${results.grossResults}")
        println("results.grossIngredients=${results.grossIngredients}")
        println("results.rawIngredients=${results.rawIngredients}")
        println("results.recipeCrafts=${results.recipeCrafts}")
        println("results.status=${results.status}")

        rg.save(File("test.state"))

        val rg2 = RecipeGroup()
        rg2.load(File("test.state"))
        RecipeMPSolverWrapper.from(rg2).also {
            it.getResults()
            it.printState()
        }
    }
}