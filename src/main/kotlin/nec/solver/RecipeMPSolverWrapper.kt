package nec.solver

import com.google.ortools.linearsolver.MPConstraint
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import nec.dbmodel.DbRecipe
import nec.dumpState
import nec.gui.calculation.GroupItemAmount
import nec.gui.calculation.RecipeGroup
import nec.setNativeLibraryPath
import org.slf4j.LoggerFactory

class RecipeMPSolverWrapper {
    private val recipes: MutableMap<Int, RecipeConfig> = hashMapOf()
    private val items: MutableMap<Int, ItemConfig> = hashMapOf()
    private val haveItems: MutableMap<Int, MPVariable> = hashMapOf()
    private val infiniteItems: MutableMap<Int, MPVariable> = hashMapOf()
    private val solver = MPSolver("SimpleLpProgram", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING)
    private val costVar = solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "cost")
    private val costExpr = solver.makeConstraint("cost")
    private val taxVar = solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "tax")
    private val taxExpr = solver.makeConstraint("tax")
    private val objective = solver.objective()

    init {
        objective.setMinimization()

        costExpr.setCoefficient(taxVar, 1.0)
        costExpr.setCoefficient(costVar, 1.0)
        costExpr.setBounds(0.0, Double.POSITIVE_INFINITY)

        taxExpr.setCoefficient(taxVar, 1.0)
        taxExpr.setBounds(0.0, Double.POSITIVE_INFINITY)
    }

    fun addRecipe(recipe: DbRecipe) {
        recipes[recipe.id] = RecipeConfig(MinimalRecipe(recipe))
    }

    fun setItemInfinite(itemId: Int, cost: Double) {
        LOG.debug("setItemInfinite($itemId, $cost)")

        val variable = infiniteItems.computeIfAbsent(itemId) {
            solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "inf-$itemId")
        }
        costExpr.setCoefficient(variable, cost)
        objective.setCoefficient(variable, cost)
        getItemConfig(itemId).constraint.setCoefficient(variable, 1.0)
    }

    fun removeItemInfinite(itemId: Int) = infiniteItems.remove(itemId)?.delete()

    fun setItemHave(itemId: Int, amount: Double) {
        if (amount == 0.0) {
            removeItemHave(itemId)
            return
        }

        val variable = haveItems.computeIfAbsent(itemId) {
            solver.makeIntVar(0.0, amount, "have-$itemId")
        }
        variable.setUb(amount)
        costExpr.setCoefficient(variable, -100.0)
        objective.setCoefficient(variable, -100.0)
        getItemConfig(itemId).constraint.setCoefficient(variable, 1.0)
    }

    fun removeItemHave(itemId: Int) {
        haveItems.remove(itemId)?.delete()
    }

    fun applyItemConfiguration(gia: GroupItemAmount) = applyItemConfiguration(gia.itemId) {
        want = gia.wantAmount
        have = gia.haveAmount
        infCost = gia.infiniteCost
        allowInfinite = gia.allowInfinite

//        constraint.setLb(want)
//        if (allowInfinite) {
//            setItemInfinite(itemId, infCost)
//        } else {
//            removeItemInfinite(itemId)
//        }
//        if (have > 0) {
//            setItemHave(itemId, have)
//        } else {
//            removeItemHave(itemId)
//        }
    }

    fun solve(): MPSolver.ResultStatus {
        addMissingItems()
        setRecipeCoefficients()

        val status = solver.solve()
//        LOG.info("status=$status")

//        LOG.info("Solution:\n${solutionString()}")
        return status
    }

    fun getResults(): RecipeSolverSolution {
        val status = solve()

        val recipeCrafts = recipes.values
            .associate { it.recipe.id to it.variable.solutionValue() }
//            .filterValues { it != 0.0 }

        val rawIngredients = infiniteItems
            .mapValues { it.value.solutionValue() }
//            .filterValues { it != 0.0 }

        val grossIngredients = HashMap<Int, Double>()
        val grossResults = HashMap<Int, Double>()
        recipes.values.forEach {
            val crafts = it.variable.solutionValue()
//            if (crafts == 0.0) return@forEach

            it.recipe.ingredients.forEach { (itemId, amount) ->
                grossIngredients.compute(itemId) { _, cur -> (cur ?: 0.0) + amount * crafts }
            }
            it.recipe.results.forEach { (itemId, amount) ->
                grossResults.compute(itemId) { _, cur -> (cur ?: 0.0) + amount * crafts }
            }
        }

        return RecipeSolverSolution(
            status,
            objective.value(),
            recipeCrafts,
            rawIngredients,
            grossIngredients,
            grossResults,
        )
    }

    fun solutionString(): String {
        val sb = StringBuilder()

        sb.appendLine("Objective value = " + objective.value())
        recipes.values.forEach {
            sb.appendLine("${it.variable.name()} = ${it.variable.solutionValue()}")
        }

        return sb.toString()
    }

    fun printState() {
        LOG.info(solver.dumpState())
    }

    private fun applyItemConfiguration(itemId: Int, block: ItemConfig.() -> Unit) {
        getItemConfig(itemId).apply {
            block()
        }
    }

    private fun getItemConfig(itemId: Int) = items.computeIfAbsent(itemId) {
        ItemConfig(it)
    }

    private fun setRecipeCoefficients() {
        recipes.values.forEach { recipe ->
            recipe.recipe.ingredients.forEach { (itemId, amount) ->
                items[itemId]?.constraint?.setCoefficient(recipe.variable, (-amount).toDouble())
            }
            // TODO this doesn't handle a recipe with same input and output
            recipe.recipe.results.forEach { (itemId, amount) ->
                items[itemId]?.constraint?.setCoefficient(recipe.variable, amount.toDouble())
            }
            objective.setCoefficient(recipe.variable, recipe.cost)
            taxExpr.setCoefficient(recipe.variable, recipe.tax)
            costExpr.setCoefficient(recipe.variable, recipe.cost)
        }
    }

    private fun addMissingItems() {
        val allIngredients = recipes.values
            .flatMap { it.recipe.ingredients.map { it.first } }
            .toSet()
            .toList()
        val allResults = recipes.values
            .flatMap { it.recipe.results.map { it.first } }
            .toSet()
            .toList()

        // flag items without a recipe producing them as implicitly infinite
        val implicitlyInfiniteItems = allIngredients - allResults
        implicitlyInfiniteItems.forEach {
            getItemConfig(it).apply {
                if (!allowInfinite) implicitlyInfinite = true
            }
        }

        // ensure there's an entry for all items in use
        (allIngredients + allResults).toSet().forEach {
            getItemConfig(it)
        }
    }

    private inner class ItemConfig(
        val itemId: Int,
        want: Double = 0.0,
        have: Double = 0.0,
        allowInfinite: Boolean = false,
        infCost: Double = 0.0,
        implicitlyInfinite: Boolean = false
    ) {
        val constraint: MPConstraint = solver.makeConstraint("item-$itemId")

        var want: Double
            get() = constraint.lb()
            set(value) = constraint.setLb(value)

        var have: Double
            get() = haveItems[itemId]?.ub() ?: 0.0
            set(value) = if (value == 0.0 || value.isNaN()) {
                removeItemHave(itemId)
            } else {
                setItemHave(itemId, value)
            }

        var allowInfinite: Boolean = allowInfinite
            set(value) {
                if (!value) {
                    removeItemInfinite(itemId)
                } else {
                    setItemInfinite(itemId, infCost)
                }
                field = value
            }

        var infCost: Double = infCost
            set(value) {
                if (allowInfinite && value != 0.0) setItemInfinite(itemId, value)
                field = value
            }

        var implicitlyInfinite: Boolean = implicitlyInfinite
            set(value) {
                if (!field && value) {
                    if (!allowInfinite) {
                        setItemInfinite(itemId, infCost)
                    }
                }
                field = value
            }

        init {
            this.want = want
            this.have = have
        }
    }

    private inner class RecipeConfig(
        val recipe: MinimalRecipe,
        cost: Double = 1.0,
        tax: Double = 1.0,
    ) {
        val variable: MPVariable = solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "recipe-${recipe.id}")

        var cost: Double
            get() = costExpr.getCoefficient(variable)
            set(value) = costExpr.setCoefficient(variable, value)
        var tax: Double
            get() = taxExpr.getCoefficient(variable)
            set(value) = taxExpr.setCoefficient(variable, value)

        init {
            this.cost = cost
            this.tax = tax
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RecipeMPSolverWrapper::class.java)

        init {
            setNativeLibraryPath("./libs")
            System.loadLibrary("jniortools")
        }

        fun from(group: RecipeGroup): RecipeMPSolverWrapper {
            val wrapper = RecipeMPSolverWrapper()

            group.recipes.values
                .map { it.recipe }
                .forEach(wrapper::addRecipe)
            group.items.values
                .forEach(wrapper::applyItemConfiguration)

            return wrapper
        }
    }
}