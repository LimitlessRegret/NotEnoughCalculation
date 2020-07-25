package nec.solver

import nec.dbmodel.DbRecipe

class MinimalRecipe(
    val id: Int,
    val ingredients: Array<Pair<Int, Int>>,
    val results: Array<Pair<Int, Int>>
) {
    constructor(recipe: DbRecipe) : this(
        recipe.id,
        recipe.ingredients
            .filter { it.item != null }
            .map { it.item!!.id to it.amount }
            .toTypedArray(),
        recipe.results
            .filter { it.item != null }
            .map { it.item!!.id to it.amount }
            .toTypedArray(),
    )
}