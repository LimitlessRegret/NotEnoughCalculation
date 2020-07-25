package nec.gui.calculation

import javafx.beans.property.SimpleObjectProperty
import nec.RecipeDatabase
import nec.gui.RecipeCalculationViewModel
import nec.gui.toIntLikeString
import tornadofx.*

class RawIngredientsView : View() {
    private val model: RecipeCalculationViewModel by inject()
    private val list = SortedFilteredList(arrayListOf<Pair<Int, Double>>().asObservable())

    override val root = tableview(list) {
        list.items.setAll(model.group.solutionProperty.get()?.rawIngredients?.map { it.key to it.value }
            ?: emptyList())
        model.group.solutionProperty.onChange {
            println("it?.rawIngredients=${it?.rawIngredients}")
            list.items.setAll(model.group.solutionProperty.get()?.rawIngredients?.map { it.key to it.value }
                ?: emptyList())
        }


        column<Pair<Int, Double>, Double>("Amount") {
            SimpleObjectProperty(it.value.second)
        }.cellFormat {
            text = item.toIntLikeString()
        }
        column<Pair<Int, Double>, String>("Item ID") {
            it.value.first.toString().toProperty()
        }
        column<Pair<Int, Double>, String>("Item") {
            RecipeDatabase.instance.getItem(it.value.first).localizedName.toProperty()

        }
    }
}