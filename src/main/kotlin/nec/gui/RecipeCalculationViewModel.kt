package nec.gui

import javafx.beans.binding.Binding
import javafx.beans.property.SimpleIntegerProperty
import nec.gui.calculation.GroupItemAmount
import nec.gui.calculation.ItemAmount
import nec.gui.calculation.RecipeGroup
import nec.gui.calculation.RecipeSelection
import org.jgrapht.alg.scoring.PageRank
import tornadofx.*
import java.io.File

class RecipeCalculationViewModel : ViewModel() {
    val itemHighlights = ItemHighlightManager()
    val group: RecipeGroup by inject()
    val maxIngredientsProperty = SimpleIntegerProperty(3)
    private var maxIngredients by maxIngredientsProperty
    val maxResultsProperty = SimpleIntegerProperty(4)
    var maxResults by maxResultsProperty
    val statusProperty = group.solutionProperty.stringBinding {
        it?.let {
            "${it.status} - Objective = ${it.objectiveValue}"
        } ?: ""
    }
    val recipeItemsListProperty = arrayListOf<RecipeSelection>().asObservable()
    val groupItemsListProperty = arrayListOf<GroupItemAmount>().asObservable()
    var solveInhibited: Boolean = false

    init {
        group.recipes.bindValuesToList(recipeItemsListProperty)
        group.items.bindValuesToList(groupItemsListProperty)

        recipeItemsListProperty.onChange {
            updateMaxIngredientsAndResults()
        }

        updateMaxIngredientsAndResults()
        solve()
    }

    fun recipeItemAmountCell(selection: RecipeSelection, isIngredient: Boolean, index: Int): Binding<ItemAmount?> {
        val itemsProperty = if (isIngredient) {
            selection.ingredientsProperty
        } else {
            selection.resultsProperty
        }

        return group.solutionProperty.objectBinding(itemsProperty) {
            val crafts = it?.recipeCrafts?.get(selection.recipe.id) ?: 0.0
            val recipeItem = itemsProperty.get()?.getOrNull(index) ?: return@objectBinding null
            val itemId = recipeItem.item.id ?: return@objectBinding null

            ItemAmount(itemId, recipeItem.amount.times(crafts), isIngredient)
        }
    }

    private fun updateMaxIngredientsAndResults() {
        maxIngredients = recipeItemsListProperty
            .map { it.recipe.oreDictIngredients.size + it.recipe.normalIngredients.size }
            .maxOrNull() ?: 0
        maxResults = recipeItemsListProperty.map { it.recipe.results.size }.maxOrNull() ?: 0
    }

    fun save(file: File) {
        group.save(
            if (!file.name.endsWith(".rg.json"))
                File(file.absolutePath + ".rg.json")
            else file
        )
    }

    fun load(file: File) {
        group.load(file)
        sortEntries()
        solve()
    }

    fun addRecipe(recipeId: Int) {
        group.addRecipe(recipeId)
        sortEntries()
        solve()
    }

    fun removeRecipe(recipeId: Int) {
        group.removeRecipe(recipeId)
        sortEntries()
        solve()
    }

    fun sortEntries() {
        solve()

        val ranking = PageRank(group.toRecipeGraph()).scores

        recipeItemsListProperty.sortByDescending {
            ranking[it.recipe]
        }

        val addedItems = HashSet<Int>()
        val sorted = ArrayList<GroupItemAmount>()
        recipeItemsListProperty.forEach { sel ->
            (sel.recipe.results + sel.ingredientsProperty.get())
                .forEach {
                    if (it.item.id !in addedItems) {
                        addedItems.add(it.item.id)
                        sorted.add(group.items.getValue(it.item.id))
                    }
                }
        }

        groupItemsListProperty.setAll(sorted)
    }

    fun solve() {
        if (solveInhibited) return
        group.solve()
    }
}
