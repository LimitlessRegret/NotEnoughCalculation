package nec

import com.google.ortools.linearsolver.MPSolver
import nec.dbmodel.tables.pojos.Item
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

val _LOG = LoggerFactory.getLogger("Utils")

inline fun <reified T : Any> timeThis(msg: String, debug: Boolean = false, crossinline block: () -> T): T {
    lateinit var returnVal: T
    val time = measureTimeMillis {
        returnVal = block()
    }
    val msg = "$msg took ${time}ms"
    if (debug) {
        _LOG.debug(msg)
    } else {
        _LOG.info(msg)
    }
    return returnVal
}

fun MPSolver.dumpState(): String {
    val sb = StringBuilder()
    val variables = variables()

    objective().also { objective ->
        sb.appendLine("Objective value: ${objective.value()}}")
        sb.append("    ")
        variables
            .map { it to objective.getCoefficient(it) }
            .filter { it.second != 0.0 }
            .forEachIndexed { idx, (variable, coefficient) ->
                if (idx != 0) sb.append(" + ")
                sb.append("$coefficient ${variable.name()}")
            }
        sb.appendLine()
    }

    sb.appendLine()
    sb.appendLine("Variables:")
    variables.forEach { variable ->
        sb.appendLine("  ${variable.name()}: value=${variable.solutionValue()}, lb=${variable.lb()}, ub=${variable.ub()}")
    }

    sb.appendLine()
    sb.appendLine("Constraints:")
    constraints().forEach { constraint ->
        sb.appendLine("  ${constraint.name()}: lb=${constraint.lb()}, ub=${constraint.ub()}")
        sb.append("    ")
        variables
            .map { it to constraint.getCoefficient(it) }
            .filter { it.second != 0.0 }
            .forEachIndexed { idx, (variable, coefficient) ->
                if (idx != 0) sb.append(" + ")
                sb.append("$coefficient ${variable.name()}")
            }
        sb.appendLine()
    }

    return sb.toString()
}

private val PROGRAMMED_CIRCUITS = setOf(
    "item.BioRecipeSelector",
    "gt.integrated_circuit",
    "item.T3RecipeSelector",
)

fun Item.isProgrammedCircuit(): Boolean = internalName in PROGRAMMED_CIRCUITS
