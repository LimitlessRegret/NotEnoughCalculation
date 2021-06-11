package nec.gui

import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.stage.StageStyle
import nec.GTOverclock
import nec.gui.calculation.CombinedItemsView
import nec.gui.calculation.RecipeTableView
import nec.gui.recipe.RecipeSearchView
import nec.solver.RecipeMPSolverWrapper
import tornadofx.*


class MasterView : View("Not Enough Calculation") {
    private val model: RecipeCalculationViewModel by inject()
    private val appSettings: AppSettings by inject()
    private val recipeTableView: RecipeTableView by inject()
    private val combinedItemsView: CombinedItemsView by inject()
    private val stateFileFilter = arrayOf(FileChooser.ExtensionFilter("Recipe group", "*.rg.json"))

    override val root = borderpane {
        top = vbox {
            menubar {
                menu("_File") {
                    isMnemonicParsing = true

                    item("Add recipe", "Shortcut+n").action {
                        find<RecipeSearchView>().openWindow(stageStyle = StageStyle.UNIFIED)
                    }
                    separator()
                    item("Clear all").action {
                        model.reset()
                    }
                    separator()
                    item("_Save", "Shortcut+s").action {
                        chooseFile("Save recipe group", stateFileFilter, null, FileChooserMode.Save)
                            .firstOrNull()
                            ?.let { model.save(it) }
                    }
                    item("_Open", "Shortcut+o").action {
                        chooseFile("Load recipe group", stateFileFilter, null, FileChooserMode.Single)
                            .firstOrNull()
                            ?.let { model.load(it) }
                    }
                }
                menu("_View") {
                    isMnemonicParsing = true
                    item("Show infinite costs").action {
                        appSettings.showInfiniteCosts = !appSettings.showInfiniteCosts
                    }
                    item("Show item ids").action {
                        appSettings.showInternalIds = !appSettings.showInternalIds
                    }
                    item("Toggle test bar").action {
                        appSettings.showTestBar = !appSettings.showTestBar
                    }
                }
                menu("_Overclock") {
                    val group = ToggleGroup()

                    isMnemonicParsing = true
                    GTOverclock.TIER_NAMES
                        .withIndex()
                        .forEach { (idx, name) ->
                            if (idx == 1) {
                                separator()
                            }

                            item(
                                if (idx == 0) {
                                    "Disabled"
                                } else {
                                    "$name (${GTOverclock.V[idx]})"
                                }
                            ) {
                                val radio = radiobutton(group = group)

                                fun select() {
                                    appSettings.overclockToTier = idx
                                }

                                radio.action { select() }
                                action { select() }

                                appSettings.overclockToTierProperty
                                    .onChange { radio.isSelected = idx == it }
                                radio.isSelected = idx == appSettings.overclockToTier
                            }
                        }
                }
            }
            hbox {
                managedWhen(appSettings.showTestBarProperty)
                visibleWhen(appSettings.showTestBarProperty)

                button("Test") {
                    action {
                        model.addRecipe(119114)
                        model.addRecipe(121186)
                        model.addRecipe(151087)
                        model.addRecipe(117813)
                        model.addRecipe(121732)
                        model.addRecipe(210977)
                        model.solve()
                    }
                }
                button("Color") {
                    action {
                        model.itemHighlights.highlightAll()
                    }
                }
                button("Remove colors") {
                    action {
                        model.itemHighlights.highlightNone()
                    }
                }
                button("Dump solver state") {
                    action {
                        RecipeMPSolverWrapper.from(model.group).also {
                            it.solve()
                            it.printState()
                        }
                    }
                }
                button("Sort").action { model.sortEntries() }
            }
        }
        center = splitpane(Orientation.VERTICAL) {
            add(recipeTableView.root)
            hbox {
                minHeight = 50.0
                prefHeight = 100.0
                usePrefHeight = true
                splitpaneConstraints {
                    isResizableWithParent = false
                }

                add(combinedItemsView.root.apply {
                    hboxConstraints {
                        hgrow = Priority.ALWAYS
                        vgrow = Priority.ALWAYS
                    }
                })
            }
        }
        bottom = hbox {
            hboxConstraints {
                alignment = Pos.BOTTOM_LEFT
                padding = Insets(4.0, 8.0, 8.0, 8.0)
                vgrow = Priority.ALWAYS
            }

            text(model.statusProperty) {

            }
        }
    }
}
