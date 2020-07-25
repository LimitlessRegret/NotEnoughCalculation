package nec.dbmodel

import nec.dbmodel.tables.pojos.Item
import nec.dbmodel.tables.pojos.RecipeItem

class DbRecipeItem(recipeItem: RecipeItem, item: Item?) {
    val recipeId = recipeItem.recipeId
    val slot = recipeItem.slot
    val amount = recipeItem.amount
    val isOutput = recipeItem.isOutput
    val item = item

    override fun toString(): String {
        return "DbRecipeItem(" +
                "recipeId=$recipeId, " +
                "slot=$slot, " +
                "isOutput=$isOutput, " +
                "amount=$amount, " +
                "item=$item" +
                ")"
    }
}