package nec.solver.legacy

import com.quantego.clp.CLP
import nec.dbmodel.DbRecipe
import nec.dbmodel.DbRecipeItem
import nec.dbmodel.tables.pojos.Item
import nec.gui.calculation.RecipeGroup
import nec.solver.RecipeSolverSolution
import org.apache.commons.math3.fraction.BigFraction
import org.apache.commons.math3.fraction.BigFractionField
import org.apache.commons.math3.linear.MatrixUtils

class RecipeSolver(
    val group: RecipeGroup
) {
    val recipes = group.recipes.values.map { it.recipe }

    val allIngredients = recipes
        .asSequence()
        .map { it.ingredients.map { it.item } }
        .flatten()
        .filterNotNull()
        .toSet()
        .toList()

    val allResults = recipes
        .asSequence()
        .map { it.results.map { it.item } }
        .flatten()
        .filterNotNull()
        .toSet()
        .toList()

    val allItems = (allIngredients + allResults).toList()

    val implicitInfiniteIngredients = allIngredients - allResults

    val matrix = MatrixUtils.createFieldMatrix(
        BigFractionField.getInstance(),
        allItems.size,
        recipes.size + implicitInfiniteIngredients.size + 1
    )

    fun Item.matrixRow() = allItems.indexOf(this)
    fun Item.infiniteCol() = recipes.size + implicitInfiniteIngredients.indexOf(this)
    fun DbRecipeItem.matrixRow() = item!!.matrixRow()
    fun DbRecipe.matrixColumn() = recipes.indexOf(this)
    val wantsColumn = recipes.size + implicitInfiniteIngredients.size

    fun solve(): RecipeSolverSolution {
        setupMatrix()

        //region Setup Solver
        val solver = CLP().minimization()
//        solver.
        val recipeVariables = recipes
            .map { solver.addVariable().name("recipe-${it.id}") } +
                implicitInfiniteIngredients.map { solver.addVariable().name("inf-${it.id}") }
        val costVar = solver.addVariable().name("cost")
        val taxVar = solver.addVariable().name("tax")
        for (i in 0 until matrix.rowDimension) {
            val expr = solver.createExpression()
            matrix.getRow(i).forEachIndexed { index, frac ->
                if (index == wantsColumn) {
                    expr.geq(frac.toDouble())
                } else {
                    expr.add(recipeVariables[index], frac.toDouble())
                }
            }
        }
        val taxExpr = solver.createExpression()
        for (index in 0 until recipes.size) {
            taxExpr.add(recipeVariables[index], 1.0)
            solver.setObjectiveCoefficient(recipeVariables[index], 1.0)
        }
        taxExpr.add(taxVar, 1.0)
        taxExpr.geq(0.0)

        val costExpr = solver.createExpression() // cost
            .add(taxVar, 1.0)
        implicitInfiniteIngredients.forEach {
            costExpr.add(recipeVariables[it.infiniteCol()], 1000.0)
            solver.setObjectiveCoefficient(recipeVariables[it.infiniteCol()], 1000.0)
        }
        costExpr.add(costVar, 1.0)
        costExpr.geq(0.0)

//        println(solver.toString())
        //endregion

        //region Solve
        val res = solver.solve()
        if (res != CLP.STATUS.OPTIMAL) {
            System.err.println("Solution found not optimal! $res")
        }

        for (col in recipeVariables) {
            println("  $col ${col.solution}")
        }
        val solution = MatrixUtils.createFieldMatrix(arrayOf(recipeVariables
            .map { BigFraction(it.solution) }
            .plus(listOf(BigFraction.ZERO))
            .toTypedArray()))

        //endregion
        println(solution)

        //region Interpret solution
        val recipeCrafts = recipeVariables
            .filter { it.toString().startsWith("recipe") }
            .associate { it.toString().split('-')[1].toInt() to it.solution }
        println(recipeCrafts)

        val rawIngredients = recipeVariables
            .filter { it.toString().startsWith("inf") }
            .associate { it.toString().split('-')[1].toInt() to it.solution }
        println(rawIngredients)
        //endregion

        return RecipeSolverSolution(
            TODO(),
            TODO(),
            recipeCrafts,
            rawIngredients,
            TODO(),
            TODO(),
        )
    }

    private fun setupMatrix() {
        recipes.forEach { recipe ->
            val column = recipe.matrixColumn()

            recipe.ingredients.forEach {
                matrix.setEntry(it.matrixRow(), column, BigFraction(it.amount).negate())
            }
            recipe.results.forEach {
                matrix.setEntry(it.matrixRow(), column, BigFraction(it.amount))
            }
        }

        implicitInfiniteIngredients.forEach {
            matrix.setEntry(it.matrixRow(), it.infiniteCol(), BigFraction.ONE)
        }

        group.items
            .values
            .filter { it.wantAmount > 0 }
            .forEach {
                matrix.setEntry(it.item.matrixRow(), wantsColumn, BigFraction(it.wantAmount))
            }
        group.items
            .values
            .filter { it.haveAmount > 0 }
            .forEach {
                val current = matrix.getEntry(it.item.matrixRow(), wantsColumn)
                matrix.setEntry(it.item.matrixRow(), wantsColumn, BigFraction(it.haveAmount).negate().add(current))
            }
    }

    companion object {
        fun solve(group: RecipeGroup): RecipeSolverSolution {
            return RecipeSolver(group).solve()
        }
    }
}