package nec.dbmodel

import nec.RecipeItemAmount
import nec.RecipeOreDictItemAmount
import nec.dbmodel.tables.pojos.Recipe
import nec.dbmodel.tables.pojos.RecipeItem


class DbRecipe(
    recipe: Recipe,
    val normalIngredients: List<RecipeItemAmount>,
    val oreDictIngredients: List<RecipeOreDictItemAmount>,
    val results: List<RecipeItemAmount>,
) {
    val id = recipe.id
    val machine = recipe.source
    val isEnabled = recipe.isEnabled
    val duration = recipe.duration
    val euT = recipe.euT

    fun uniqueOreDictItemsInSlot(slot: Int) = oreDictIngredients[slot]
        .oreDicts
        .flatMap { it.itemsIds.asList() }
        .toSet()

    override fun toString(): String {
        return "DbRecipe(" +
                "id=$id, " +
                "machine='$machine', " +
//                "isEnabled=$isEnabled, " +
                "duration=$duration, " +
                "euT=$euT, " +
                "normalIngredients=$normalIngredients, " +
                "oreDictIngredients=$oreDictIngredients, " +
                "results=$results" +
                ")"
    }
}

private fun DbRecipeItem.copyWithAmount(totalAmount: Int) = RecipeItem(
    recipeId,
    item?.id,
    oreDictId,
    slot,
    totalAmount,
    chance,
    isOutput
)

