@file:UseSerializers(
    SimpleDoublePropertySerializer::class,
    SimpleBooleanPropertySerializer::class,
    SimpleIntegerPropertySerializer::class,
)

package nec.gui.calculation

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import nec.RecipeDatabase
import nec.persistence.SimpleBooleanPropertySerializer
import nec.persistence.SimpleDoublePropertySerializer
import nec.persistence.SimpleIntegerPropertySerializer
import tornadofx.doubleBinding
import tornadofx.getValue
import tornadofx.setValue
import tornadofx.toProperty

@Serializable
class GroupItemAmount(
    val itemId: Int
) {
    @Transient
    val itemIdProperty = itemId.toProperty()

    @Transient
    val itemProperty = SimpleObjectProperty(RecipeDatabase.instance.getItem(itemId))
    var item by itemProperty

    @SerialName("want")
    val wantAmountProperty = SimpleDoubleProperty(0.0)
    var wantAmount by wantAmountProperty

    @SerialName("have")
    val haveAmountProperty = SimpleDoubleProperty(0.0)
    var haveAmount by haveAmountProperty

    @SerialName("isInfinite")
    val allowInfiniteProperty = SimpleBooleanProperty(false)
    var allowInfinite by allowInfiniteProperty

    @SerialName("infCost")
    val infiniteCostProperty = SimpleDoubleProperty(0.0)
    var infiniteCost by infiniteCostProperty

    @Transient
    val totalUsedAmountProperty = SimpleDoubleProperty(0.0)
    var totalUsedAmount by totalUsedAmountProperty

    @Transient
    val totalProducedAmountProperty = SimpleDoubleProperty(0.0)
    var totalProducedAmount by totalProducedAmountProperty

    @Transient
    val byproductAmountProperty =
        totalUsedAmountProperty.doubleBinding(totalProducedAmountProperty, wantAmountProperty, haveAmountProperty) {
            val byproduct = totalProducedAmount - totalUsedAmount - wantAmount + haveAmount
            if (byproduct < 0) return@doubleBinding 0.0
            return@doubleBinding byproduct
        }

    @Transient
    val rawIngredientAmountProperty = totalUsedAmountProperty.doubleBinding(totalProducedAmountProperty) {
        val toCraft = totalUsedAmount - totalProducedAmount
        if (toCraft < 0) return@doubleBinding 0.0
        return@doubleBinding toCraft
    }

    val isZero
        get() = wantAmount == 0.0 &&
                haveAmount == 0.0 &&
                totalProducedAmount == 0.0 &&
                totalUsedAmount == 0.0
}