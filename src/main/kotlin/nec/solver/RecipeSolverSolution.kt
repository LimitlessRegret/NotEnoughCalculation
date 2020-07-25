package nec.solver

import com.google.ortools.linearsolver.MPSolver

data class RecipeSolverSolution(
    val status: MPSolver.ResultStatus,
    val objectiveValue: Double,
    val recipeCrafts: Map<Int, Double>,
    val rawIngredients: Map<Int, Double>,
    val grossIngredients: Map<Int, Double>,
    val grossResults: Map<Int, Double>,
)