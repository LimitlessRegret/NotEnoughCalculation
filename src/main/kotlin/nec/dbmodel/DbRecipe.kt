package nec.dbmodel

import nec.dbmodel.tables.pojos.Recipe
import nec.dbmodel.tables.pojos.RecipeItem

class DbRecipe(recipe: Recipe, items: Collection<DbRecipeItem>) {
    val id = recipe.id
    val machine = recipe.source
    val isEnabled = recipe.isEnabled
    val duration = recipe.duration
    val euT = recipe.euT
    val rawIngredients = items.filter { !it.isOutput }
    val rawResults = items.filter { it.isOutput }
    val ingredients = rawIngredients
        .groupBy { it.item }
        .map { (item, ingredients) ->
            val totalAmount = ingredients.sumBy { it.amount }
            DbRecipeItem(ingredients.first().copyWithAmount(totalAmount), item)
        }
    val results = rawResults
        .groupBy { it.item }
        .map { (item, results) ->
            val totalAmount = results.sumBy { it.amount }
            DbRecipeItem(results.first().copyWithAmount(totalAmount), item)
        }

    override fun toString(): String {
        return "DbRecipe(" +
                "id=$id, " +
                "machine='$machine', " +
//                "isEnabled=$isEnabled, " +
                "duration=$duration, " +
                "euT=$euT, " +
                "ingredients=$ingredients, " +
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

