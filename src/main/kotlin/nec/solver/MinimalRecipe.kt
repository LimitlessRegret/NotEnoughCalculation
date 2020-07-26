package nec.solver

import nec.gui.calculation.RecipeSelection

class MinimalRecipe(
    val id: Int,
    val ingredients: Array<Pair<Int, Int>>,
    val results: Array<Pair<Int, Int>>
) {
    constructor(selection: RecipeSelection) : this(
        selection.recipeId,
        selection.ingredientsProperty
            .get()
            .map { it.item.id to it.amount }
            .toTypedArray(),
        selection.recipe.results
            .map { it.item.id to it.amount }
            .toTypedArray(),
    )
}