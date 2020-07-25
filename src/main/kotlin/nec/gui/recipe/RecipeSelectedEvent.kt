package nec.gui.recipe

import tornadofx.FXEvent

data class RecipeSelectedEvent(
    val recipe: MachineRecipeGroup
) : FXEvent()
