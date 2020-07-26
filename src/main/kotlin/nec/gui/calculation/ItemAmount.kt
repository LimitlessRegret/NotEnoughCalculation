package nec.gui.calculation

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import nec.RecipeDatabase
import nec.gui.toIntLikeString
import nec.isProgrammedCircuit
import tornadofx.getValue
import tornadofx.setValue
import tornadofx.stringBinding

class ItemAmount(
    itemId: Int,
    amount: Double,
    val isIngredient: Boolean,
) {
    val itemIdProperty = SimpleIntegerProperty(itemId)
    var itemId by itemIdProperty

    val amountProperty = SimpleDoubleProperty(amount)
    var amount by amountProperty

    val itemProperty = SimpleObjectProperty(RecipeDatabase.instance.getItem(itemId))
    var item by itemProperty

    val displayProperty = amountProperty.stringBinding(itemIdProperty) {
        val name = item.localizedName
        "${amount.toIntLikeString()}x $name" + (if (item?.damage != null && item?.isProgrammedCircuit() == true) " (${item.damage})" else "")
    }
}