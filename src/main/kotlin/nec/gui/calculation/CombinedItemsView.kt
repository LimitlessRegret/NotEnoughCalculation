package nec.gui.calculation

import javafx.beans.property.Property
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.TextFieldTableCell
import javafx.util.converter.NumberStringConverter
import nec.gui.*
import tornadofx.*

class CombinedItemsView : View() {
    private val model: RecipeCalculationViewModel by inject()
    private val appSettings: AppSettings by inject()

    override val root = tableview(model.groupItemsListProperty) {
        isEditable = true
        column("Item id", GroupItemAmount::itemIdProperty) {
            isVisible = appSettings.showInternalIds
            appSettings.showInternalIdsProperty.onChange { isVisible = it }
        }
        column("Item", GroupItemAmount::itemProperty) {
            minWidth = 150.0
            prefWidth = 250.0
            usePrefWidth = true
            cellFormat {
                text = item.localizedName
                setDefaultTableCellStyles()
                backgroundProperty().bind(model.itemHighlights.getBackgroundFor(item.id))
            }
        }
        column("Have", GroupItemAmount::haveAmountProperty).applyCommonEditable()
        column("Want", GroupItemAmount::wantAmountProperty).applyCommonEditable()
        column("Raw input", GroupItemAmount::rawIngredientAmountProperty).applyCommon()
        column("Σ Used", GroupItemAmount::totalUsedAmountProperty).applyCommon()
        column("Σ Produced", GroupItemAmount::totalProducedAmountProperty).applyCommon()
        column("By-product", GroupItemAmount::byproductAmountProperty).applyCommon()
        column("Infinite", GroupItemAmount::allowInfiniteProperty).useCheckbox()
            .setOnEditCommit {
                it.rowValue.allowInfinite = it.newValue ?: false
                model.solve()
            }
        column("Inf cost", GroupItemAmount::infiniteCostProperty) {
            applyCommonEditable()
            isVisible = appSettings.showInfiniteCosts
            appSettings.showInfiniteCostsProperty.onChange { isVisible = it }
        }
        setOnMouseClicked {
            val gia: GroupItemAmount = it.findItem() ?: return@setOnMouseClicked

            model.itemHighlights.onlyHighlight(listOf(gia.itemId))
        }
    }

    private fun TableColumn<GroupItemAmount, Number?>.applyCommonEditable() {
        setCellFactory {
            val cell = TextFieldTableCell<GroupItemAmount, Number?>(NumberStringConverter())
            cell.setDefaultTableCellStyles()

            cell
        }
        setOnEditCommit {
            val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<Number?>
            property.value = it.newValue
            model.solve()
        }
    }

    private fun TableColumn<GroupItemAmount, Number?>.applyCommon() {
        cellFormat {
            setDefaultTableCellStyles()
            text = item?.toDouble()?.toIntLikeString()
        }
    }
}

