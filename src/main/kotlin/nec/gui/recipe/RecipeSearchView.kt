package nec.gui.recipe

import com.sun.javafx.scene.control.skin.ListViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Priority
import nec.dbmodel.DbRecipe
import nec.gui.RecipeCalculationViewModel
import nec.gui.activateOn
import nec.gui.recipe.ItemSearchMode.*
import nec.isProgrammedCircuit
import tornadofx.*


class RecipeSearchView : Fragment("Recipe Search") {
    private val model = RecipeSearchViewModel()
    private val calcModel: RecipeCalculationViewModel by inject()

    init {
        val query = params["query"] as? String
        if (query != null) {
            model.itemQueryProperty.set(query)
        }
        val searchMode = params["searchMode"] as? ItemSearchMode
        if (searchMode != null) {
            model.searchMode = searchMode
        }
    }

    override val root = vbox {
        hbox {
            hboxConstraints {
                alignment = Pos.CENTER_LEFT
                paddingAll = 4.0
            }

            text("Item:") {
                hboxConstraints {
                    marginLeft = 4.0
                    marginRight = 4.0
                }
            }
            textfield(model.itemQueryProperty) {
                hboxConstraints {
                    marginRight = 8.0
                }
            }

            togglegroup {
                fun addFilterToggle(label: String, combo: String, mode: ItemSearchMode) {
                    radiobutton("_$label") {
                        activateOn(this@RecipeSearchView, combo)
                        action { if (isSelected) model.searchMode = mode }
                        isMnemonicParsing = true
                        isSelected = mode == model.searchMode
                    }
                }

                addFilterToggle("Input", "Alt+i", INPUT_ONLY)
                addFilterToggle("Output", "Alt+o", OUTPUT_ONLY)
                addFilterToggle("Either", "Alt+e", ANY)
            }
        }

        splitpane(Orientation.HORIZONTAL) {
            hboxConstraints {
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
            }

            listview(model.machineList) {
                minWidth = 100.0
                prefWidth = 200.0
                maxWidth = 300.0
                usePrefWidth = true
                splitpaneConstraints {
                    isResizableWithParent = false
                }
                addEventFilter(ScrollEvent.SCROLL) {
                    if (it.deltaY < 0) {
                        model.selectMachine(1)
                    } else {
                        model.selectMachine(-1)
                    }
                    it.consume()
                }

                bindSelected(model.selectedMachineProperty)
                model.selectedMachineProperty.onChange {
                    selectionModel.select(it)

                    val ts = skin as ListViewSkin<*>
                    val vf = ts.children[0] as VirtualFlow<*>

                    val priorSelection = selectionModel.selectedIndices.firstOrNull() ?: 0
                    val itemIndex = items.indexOf(it)

                    val scrollBufferSpace = 3
                    if (itemIndex < scrollBufferSpace || itemIndex >= items.size - scrollBufferSpace) {
                        /* Do nothing */
                    } else if (itemIndex > priorSelection) {
                        vf.show(itemIndex + scrollBufferSpace)
                    } else {
                        vf.show(itemIndex - scrollBufferSpace)
                    }
                }

                cellFormat {
                    text = "${item.machine} (${item.recipes.size})"
                }
            }
            vbox {
                addEventFilter(ScrollEvent.SCROLL) {
                    if (it.deltaY < 0) {
                        model.selectRecipe(1)
                    } else {
                        model.selectRecipe(-1)
                    }
                }

                hbox {
                    alignment = Pos.CENTER
                    button("<") {
                        action { model.selectRecipe(-1) }
                        enableWhen(model.machineHasPreviousRecipeProperty)
                    }
                    text(model.selectedRecipeIndexProperty.stringBinding(model.currentMachineRecipeCountProperty) {
                        "${(it?.toInt() ?: -1) + 1} / ${model.currentMachineRecipeCountProperty.get()}"
                    }) {
                        hboxConstraints {
                            marginLeft = 8.0
                            marginRight = 8.0
                        }
                    }
                    button(">") {
                        action { model.selectRecipe(1) }
                        enableWhen(model.machineHasNextRecipeProperty)
                    }
                }

                scrollpane(fitToWidth = true) {
                    hboxConstraints {
                        hgrow = Priority.ALWAYS
                        vgrow = Priority.ALWAYS
                    }

                    text(model.selectedRecipeProperty.stringBinding {
                        it?.let { it1 -> renderRecipe(it1) }
                    }) {
                        isFocusTraversable = false
                        isDisable = true
                    }
                }
            }
        }
        borderpane {
            left {
                hbox {
                    alignment = Pos.BOTTOM_LEFT
                    padding = Insets(4.0, 8.0, 8.0, 8.0)
                    vgrow = Priority.ALWAYS
                    text(model.statusTextProperty)
                }
            }
            right {
                hbox {
                    alignment = Pos.CENTER_RIGHT
                    padding = Insets(4.0, 8.0, 8.0, 8.0)

                    button("Cancel") {
                        hboxConstraints {
                            marginRight = 8.0
                        }
                        action {
                            close()
                        }
                    }
                    button("Select") {
                        enableWhen(model.selectedRecipeProperty.booleanBinding { it != null })
                        action {
                            calcModel.addRecipe(model.selectedRecipeProperty.value!!.id)
                            close()
                        }
                    }
                }
            }
        }
    }

    private fun renderRecipe(recipe: DbRecipe): String {
        val sb = StringBuilder()
        sb.appendLine("Recipe ${recipe.id}")
        sb.appendLine()

        sb.appendLine("Input:")
        recipe.normalIngredients.forEach {
            sb.append("    ${it.amount}x ${it.item.localizedName}")
            if (it.item.isProgrammedCircuit()) {
                sb.append(" (${it.item.damage})")
            }
            sb.appendLine()
        }
        recipe.oreDictIngredients.forEach {
            sb.appendLine("    ${it.amount}x ${it.oreDicts.joinToString(", ") { it.name }}")
        }

        sb.appendLine()
        sb.appendLine("Output:")
        recipe.results.forEach { result ->
            sb.appendLine("    ${result.amount}x ${result.item.localizedName}")
        }

        return sb.toString()
    }
}
