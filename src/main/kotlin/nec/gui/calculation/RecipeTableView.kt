package nec.gui.calculation

import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.text.Font
import javafx.scene.text.FontSmoothingType
import javafx.stage.StageStyle
import nec.gui.*
import nec.gui.recipe.ItemSearchMode
import nec.gui.recipe.RecipeSearchView
import tornadofx.*

class RecipeTableView : View() {
    private val model: RecipeCalculationViewModel by inject()
    private val appSettings: AppSettings by inject()
    private lateinit var ingredientColumns: List<TableColumn<RecipeSelection, ItemAmount?>>
    private lateinit var resultColumns: List<TableColumn<RecipeSelection, ItemAmount?>>

    override val root = tableview(model.recipeItemsListProperty) {
        smartResize()
    
        column("Id", RecipeSelection::recipeIdProperty) {
            isVisible = appSettings.showInternalIds
            appSettings.showInternalIdsProperty.onChange { isVisible = it }
        }
        style {
            this.font = Font.font("Verdana")

            fontSmoothingType = FontSmoothingType.GRAY
            this.fontSize = Dimension(11.0, Dimension.LinearUnits.px)
        }
        column("Crafts") { cdf: TableColumn.CellDataFeatures<RecipeSelection, String> ->
            model.group.solutionProperty.stringBinding(cdf.value.recipeIdProperty) {
                it?.recipeCrafts?.get(cdf.value.recipe.id)?.toIntLikeString()
            }
        }
        column("EU/t", RecipeSelection::euT)

        column("Machine") { cdf: TableColumn.CellDataFeatures<RecipeSelection, String> ->
            cdf.value.recipe.let { recipe ->
                when (recipe.euT) {
                    null -> recipe.machine
                    else -> "${recipe.machine} (${recipe.euT.toPowerTier()})"
                }
            }.toProperty()
        }
//        column("Duration", RecipeSelection::durationTicks)
//        column("eu/t", RecipeSelection::euT)
        ingredientColumns = addItemColumns(true)
        resultColumns = addItemColumns(false)

        this.setOnMouseClicked {
            val recipe = it.findItem<RecipeSelection>()
            val itemAmount = it.findItem<ItemAmount>()

            when {
                recipe == null -> return@setOnMouseClicked
                it.clickCount == 2 -> return@setOnMouseClicked onDoubleClick(recipe, itemAmount)
                it.clickCount == 1 -> return@setOnMouseClicked if (itemAmount != null) {
                    onRecipeItemClick(recipe, itemAmount)
                } else {
                    onRecipeRowClick(recipe)
                }
            }
        }
        setOnKeyPressed {
            val selected = selectionModel.selectedItem
                ?: return@setOnKeyPressed

            if (it.code == KeyCode.DELETE) {
                model.removeRecipe(selected.recipeId)
            }
        }
    }

    private fun onDoubleClick(recipe: RecipeSelection, itemAmount: ItemAmount?) {
        val item = itemAmount?.item ?: return

        find<RecipeSearchView>(
            "query" to "item:${item.id}",
            "searchMode" to if (itemAmount.isIngredient) ItemSearchMode.OUTPUT_ONLY else ItemSearchMode.INPUT_ONLY
        ).openWindow(stageStyle = StageStyle.UNIFIED)
    }

    private fun onRecipeItemClick(recipe: RecipeSelection, itemAmount: ItemAmount) {
        model.itemHighlights.onlyHighlight(listOf(itemAmount.itemId))
    }

    private fun onRecipeRowClick(recipe: RecipeSelection) {
        model.itemHighlights.onlyHighlight(
            (recipe.ingredientsProperty.get().map { it.item.id } +
                    recipe.recipe.results.map { it.item.id })
                .filterNotNull()
        )
    }

    private fun TableView<RecipeSelection>.addItemColumns(isIngredient: Boolean): List<TableColumn<RecipeSelection, ItemAmount?>> {
        val maxXProperty = if (isIngredient) model.maxIngredientsProperty else model.maxResultsProperty
        val titlePrefix = if (isIngredient) "Ingredient" else "Result"

        return (0 until 16).map { idx ->
            column("$titlePrefix ${idx + 1}") { cdf: TableColumn.CellDataFeatures<RecipeSelection, ItemAmount?> ->
                model.recipeItemAmountCell(cdf.value, isIngredient, idx)
            }.apply {
                visibleProperty().bindBidirectional(
                    maxXProperty.select {
                        (idx < it!!.toInt()).toProperty()
                    }
                )
                cellFormat {
                    itemProperty().onChange {
                        if (it == null) {
                            removeClass(Styles.itemRawInput, Styles.itemOutput)
                            backgroundProperty().unbind()
                            backgroundProperty().set(null)
                        }
                    }
                    setDefaultTableCellStyles()
                    removeClass(Styles.itemRawInput, Styles.itemOutput)
                    text = item?.displayProperty?.get()
                    model.group.items[item?.itemId]?.let { groupItem ->
                        if (!isIngredient && groupItem.wantAmount > 0) {
                            addClass(Styles.itemOutput)
                        }
                        if (isIngredient && groupItem.rawIngredientAmountProperty.get() > 0) {
                            addClass(Styles.itemRawInput)
                        }
                    }

                    if (item != null) {
                        backgroundProperty().bind(model.itemHighlights.getBackgroundFor(item!!.itemId))
                    } else {
                        backgroundProperty().unbind()
                        backgroundProperty().set(null)
                    }
                }
            }
        }
    }
}

private fun Int.toPowerTier(): String {
    return when {
        this <= 8 -> "ULV"
        this <= 32 -> "LV"
        this <= 128 -> "MV"
        this <= 512 -> "HV"
        this <= 2048 -> "EV"
        this <= 8192 -> "IV"
        this <= 32768 -> "LuV"
        this <= 131072 -> "ZPM"
        this <= 524288 -> "UV"
        this <= 2097152 -> "UHV"
        this <= 8388608 -> "UEV"
        this <= 33554432 -> "UIV"
        this <= 134217728 -> "UMV"
        this <= 536870912 -> "UXV"
        this <= 1073741824 -> "OpV"
        this <= 2147483647 -> "MAX"
        else -> "??"
    }
}
