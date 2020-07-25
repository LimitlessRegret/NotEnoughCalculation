package nec.gui.calculation

import javafx.beans.property.SimpleIntegerProperty
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nec.RecipeDatabase
import tornadofx.*

@Serializable
class RecipeSelection(
    val recipeId: Int
) {
    @Transient
    val recipeIdProperty = recipeId.toProperty()
    @Transient
    val recipe = RecipeDatabase.instance.getRecipe(recipeId)
    val slotOverrides = HashMap<Int, Int>() // TODO
    @Transient
    val cachedSolutionProperty = SimpleIntegerProperty()
    val cachedSolution by cachedSolutionProperty

    @Transient
    val machineProperty = recipe.machine.toProperty()
    @Transient
    val ingredientsProperty = recipe.ingredients.toProperty()
    @Transient
    val resultsProperty = recipe.results.toProperty()
    @Transient
    val durationTicks = (recipe.duration ?: 0).toProperty()
    @Transient
    val durationSeconds = ((recipe.duration ?: -1) / 20.0).toProperty()
    @Transient
    val euT = (recipe.euT ?: 0).toProperty()
}