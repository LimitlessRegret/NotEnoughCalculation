package nec.gui.calculation

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nec.RecipeDatabase
import nec.RecipeItemAmount
import tornadofx.asObservable
import tornadofx.getValue
import tornadofx.toProperty

@Serializable
class RecipeSelection(
    val recipeId: Int
) {
    @Transient
    val recipeIdProperty = recipeId.toProperty()

    @Transient
    val recipe = RecipeDatabase.instance.getRecipe(recipeId)

    @SerialName("slotOverrides")
    private val slotOverrides_backing = HashMap<Int, Int>()

    @Transient
    val slotOverrides = slotOverrides_backing.asObservable()

    @Transient
    val cachedSolutionProperty = SimpleIntegerProperty()
    val cachedSolution by cachedSolutionProperty

    @Transient
    val machineProperty = recipe.machine.toProperty()

    @Transient
    val ingredientsProperty = Bindings.createObjectBinding({
        recipe.normalIngredients + recipe.oreDictIngredients.flatMapIndexed { idx, odi ->
            val item = if (idx in slotOverrides) {
                RecipeDatabase.instance.getItem(slotOverrides.getValue(idx))
            } else {
                odi.oreDicts
                    .asSequence()
                    .map { it.items.firstOrNull() }
                    .firstOrNull()
                    ?: return@flatMapIndexed emptyList()
            }

            listOf(RecipeItemAmount(item, odi.amount, null, oreDictSlot = idx))
        }
    }, slotOverrides)

    @Transient
    val resultsProperty = recipe.results.toProperty()

    @Transient
    val durationTicks = (recipe.duration ?: 0).toProperty()

    @Transient
    val durationSeconds = ((recipe.duration ?: -1) / 20.0).toProperty()

    @Transient
    val euT = (recipe.euT ?: 0).toProperty()
}