package nec.gui.oredict

import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import nec.dbmodel.tables.pojos.Item
import nec.gui.RecipeCalculationViewModel
import nec.gui.scrollSelectionWithWheel
import tornadofx.*


class OreDictChoiceView : Fragment("Oredict Selection") {
    private val model = OreDictChoiceViewModel()
    private val calcModel: RecipeCalculationViewModel by inject()

    init {
        val oreDicts = params["oreDicts"] as? List<Int>
        if (oreDicts != null) {
            model.oreDictsProperty.set(oreDicts)
        }
        val forRecipeId = params["forRecipeId"] as? Int
        if (forRecipeId != null) {
            model.forRecipeIdProperty.set(forRecipeId)
        }
        val forOreSlot = params["forOreSlot"] as? Int
        if (forOreSlot != null) {
            model.forOreSlotProperty.set(forOreSlot)
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
        }

        splitpane(Orientation.HORIZONTAL) {
            hboxConstraints {
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
            }

            listview(model.itemsInDicts) {
                minWidth = 100.0
                prefWidth = 200.0
                maxWidth = 300.0
                usePrefWidth = true
                splitpaneConstraints {
                    isResizableWithParent = false
                }
                scrollSelectionWithWheel(model.selectedOreDictGroupProperty)

                cellFormat {
                    text = "${item.dictName} (${item.items.size})"
                }
            }

            listview<Item> {
                itemsProperty().bind(model.selectedOreDictGroupProperty.objectBinding {
                    (it?.items?.toList() ?: emptyList()).asObservable()
                })

                minWidth = 200.0
                prefWidth = 300.0
                usePrefWidth = true
                scrollSelectionWithWheel(model.selectedItemProperty)

                cellFormat {
                    text = model.formatItemName(item)
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
                        enableWhen(model.selectedItemProperty.booleanBinding { it != null })
                        action {
                            calcModel.setOreSlotOverride(
                                model.forRecipeIdProperty.get(),
                                model.forOreSlotProperty.get(),
                                model.selectedItemProperty.get().id,
                            )
                            close()
                        }
                    }
                }
            }
        }
    }

}
