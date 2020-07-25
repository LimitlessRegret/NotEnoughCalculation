package nec.solver.legacy

import com.google.ortools.linearsolver.MPSolver
import nec.dbmodel.DbRecipe
import nec.dbmodel.DbRecipeItem
import nec.dbmodel.tables.pojos.Item
import nec.dumpState
import nec.gui.calculation.RecipeGroup
import nec.solver.RecipeSolverSolution
import org.apache.commons.math3.fraction.BigFraction
import org.apache.commons.math3.fraction.BigFractionField
import org.apache.commons.math3.linear.MatrixUtils

class RecipeSolverIP(
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

    val haveSomeItemsMap = group.items.values
        .filter { it.haveAmount > 0 }
        .associate { it.item!! to it.haveAmount }
    val haveSomeItems = group.items.values
        .filter { it.haveAmount > 0 }
        .map { it.item!! }

    val allItems = (allIngredients + allResults + haveSomeItems).toSet().toList()

    val implicitInfiniteIngredients = allIngredients - allResults

    val matrix = MatrixUtils.createFieldMatrix(
        BigFractionField.getInstance(),
        allItems.size,
        recipes.size + implicitInfiniteIngredients.size + haveSomeItems.size + 1
    )

    fun Item.matrixRow() = allItems.indexOf(this)
    fun Item.infiniteCol() = recipes.size + implicitInfiniteIngredients.indexOf(this)
    fun Item.haveSomeCol() = recipes.size + implicitInfiniteIngredients.size + haveSomeItems.indexOf(this)
    fun DbRecipeItem.matrixRow() = item!!.matrixRow()
    fun DbRecipe.matrixColumn() = recipes.indexOf(this)
    val wantsColumn = recipes.size + implicitInfiniteIngredients.size + haveSomeItems.size

    fun solve(): RecipeSolverSolution {
        setupMatrix()

        System.loadLibrary("jniortools");

        val solver = MPSolver("SimpleLpProgram", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING)
        val objective = solver.objective()
        objective.setMinimization()

        val recipeVariables = recipes.map {
            solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "recipe-${it.id}")
        } + implicitInfiniteIngredients.map {
            solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "inf-${it.id}")
        } + haveSomeItems.map {
            solver.makeIntVar(0.0, haveSomeItemsMap.getValue(it), "have-${it.id}")
        }
//        recipeVariables.forEach {
//            println(it.name())
//        }
//        println(matrix.rowDimension)
//        println(matrix.columnDimension)
        val costVar = solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "cost")
        val taxVar = solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "tax")
        for (i in 0 until matrix.rowDimension) {
            val expr = solver.makeConstraint("item-${allItems[i].id}")
            matrix.getRow(i).forEachIndexed { index, frac ->
                if (index == wantsColumn) {
                    expr.setBounds(frac.toDouble(), Double.POSITIVE_INFINITY)
                } else {
                    expr.setCoefficient(recipeVariables[index], frac.toDouble())
                }
            }
        }
        val taxExpr = solver.makeConstraint("tax")
        for (index in 0 until recipes.size) {
            taxExpr.setCoefficient(recipeVariables[index], 1.0)
            objective.setCoefficient(recipeVariables[index], 1.0)
        }
        taxExpr.setCoefficient(taxVar, 1.0)
        taxExpr.setBounds(0.0, Double.POSITIVE_INFINITY)

        val costExpr = solver.makeConstraint("cost") // cost
        costExpr.setCoefficient(taxVar, 1.0)
        implicitInfiniteIngredients.forEach {
            costExpr.setCoefficient(recipeVariables[it.infiniteCol()], 1000.0)
            objective.setCoefficient(recipeVariables[it.infiniteCol()], 1000.0)
        }
        costExpr.setCoefficient(costVar, 1.0)
        costExpr.setBounds(0.0, Double.POSITIVE_INFINITY)

//        val existingItemsConstraint = solver.makeConstraint()
        group.items.values
            .filter { it.haveAmount > 0 }
            .forEach { item ->
//                existingItemsConstraint.setCoefficient(recipeVariables[item.item.matrixRow()], item.haveAmount)
//                val haveVar = solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "have-${item.itemId}")
//                val expr = solver.makeConstraint()
//                val itemVar = recipeVariables[item.item.matrixRow()]
//                println(itemVar)
//                expr.setCoefficient(haveVar, -1.0)
//                expr.setCoefficient(itemVar, 1.0)
//                expr.setBounds(-item.haveAmount, Double.POSITIVE_INFINITY)
//                costExpr.setCoefficient(haveVar, -1.0)
//                objective.setCoefficient(haveVar, -1.0)
                costExpr.setCoefficient(recipeVariables[item.item!!.haveSomeCol()], -100.0)
                objective.setCoefficient(recipeVariables[item.item!!.haveSomeCol()], -100.0)
            }
//        existingItemsConstraint.setBounds(0.0, 1.0)


        val results = solver.solve()
//        println("results=$results")

//        println("Solution:")
//        println("Objective value = " + objective.value())
//        recipeVariables.forEach {
//            println("${it.name()} = ${it.solutionValue()}")
//        }

//        println("\nAdvanced usage:")
//        println("Problem solved in " + solver.wallTime() + " milliseconds")
//        println("Problem solved in " + solver.iterations() + " iterations")
//        println("Problem solved in " + solver.nodes() + " branch-and-bound nodes")

        //region Interpret solution
        val recipeCrafts = recipeVariables
            .filter { it.name().startsWith("recipe") }
            .associate { it.name().split('-')[1].toInt() to it.solutionValue() }
//        println(recipeCrafts)

        val rawIngredients = recipeVariables
            .filter { it.name().startsWith("inf") }
            .associate { it.name().split('-')[1].toInt() to it.solutionValue() }
//        println(rawIngredients)

//        println(matrix)

        // skip last column which is wants
        (0 until matrix.columnDimension - 1).forEach { colIndex ->
            val resultVar = recipeVariables[colIndex]
            matrix.setColumnVector(
                colIndex,
                matrix.getColumnVector(colIndex).mapMultiplyToSelf(BigFraction(resultVar.solutionValue()))
            )
        }

        val grossIngredients = HashMap<Int, Double>()
        val grossResults = HashMap<Int, Double>()
        allItems.forEach { item ->
            val itemRow = item.matrixRow()
            val row = matrix.getRow(itemRow)
//            println("${item.localizedName} ${row.contentToString()}")

            val (pos, neg) = row.dropLast(implicitInfiniteIngredients.size + 1).partition { it >= BigFraction.ZERO }
            val grossProduce = pos.reduceOrNull { acc, value ->
                (acc ?: BigFraction.ZERO).add(value)
            } ?: BigFraction.ZERO
            val grossConsume = neg.reduceOrNull { acc, value ->
                (acc ?: BigFraction.ZERO).add(value)
            } ?: BigFraction.ZERO
            grossResults[item.id] = grossProduce.toDouble()
            grossIngredients[item.id] = grossConsume.negate().toDouble()
        }
        //endregion

//        println(solver.dumpState())

        return RecipeSolverSolution(
            results,
            objective.value(),
            recipeCrafts,
            rawIngredients,
            grossIngredients,
            grossResults,
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

        haveSomeItemsMap.forEach { (item, amount) ->
            matrix.setEntry(item.matrixRow(), item.haveSomeCol(), BigFraction.ONE)
        }
        group.items
            .values
            .filter { it.wantAmount > 0 }
            .forEach {
                matrix.setEntry(it.item.matrixRow(), wantsColumn, BigFraction(it.wantAmount))
            }
//        group.items
//            .values
//            .filter { it.haveAmount > 0 }
//            .forEach {
//                val current = matrix.getEntry(it.item.matrixRow(), wantsColumn)
//                matrix.setEntry(it.item.matrixRow(), wantsColumn, BigFraction(it.haveAmount).negate().add(current))
//            }
    }

    companion object {
        fun solve(group: RecipeGroup): RecipeSolverSolution {
            return RecipeSolverIP(group).solve()
        }
    }
}