package nec.gui.calculation

import javafx.beans.property.SimpleObjectProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import nec.gui.AppSettings
import nec.solver.RecipeMPSolverWrapper
import nec.solver.RecipeSolverSolution
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.slf4j.LoggerFactory
import tornadofx.Controller
import tornadofx.asObservable
import tornadofx.getValue
import tornadofx.setValue
import java.io.File

@Serializable
class RecipeGroup : Controller() {
    private val appSettings: AppSettings by inject()

    @SerialName("recipes")
    private val recipesBackingField = HashMap<Int, RecipeSelection>()

    @Transient
    val recipes = recipesBackingField.asObservable()

    @SerialName("items")
    val itemsBackingField = HashMap<Int, GroupItemAmount>()

    @Transient
    val items = itemsBackingField.asObservable()

    @Transient
    val solutionProperty = SimpleObjectProperty<RecipeSolverSolution>()
    private var solution by solutionProperty

    fun solve() {
        if (recipes.isEmpty()) return

        val wrapper = RecipeMPSolverWrapper.from(this, appSettings.integerSolution)
        solution = wrapper.getResults()

        // reset solution values in GIA
        items.values.forEach {
            it.totalUsedAmount = 0.0
            it.totalProducedAmount = 0.0
        }

        solution.grossIngredients.forEach(items) { gia, amount ->
            gia.totalUsedAmount = amount
        }
        solution.grossResults.forEach(items) { gia, amount ->
            gia.totalProducedAmount = amount
        }
    }

    fun save(file: File) = file.writeText(save())
    fun save() = Json {}.encodeToString(serializer(), this)
    fun load(file: File) = load(file.readText())
    fun load(data: String) = load(Json {}.decodeFromString(serializer(), data))
    fun load(other: RecipeGroup) {
        items.clear()
        recipes.clear()
        recipes.putAll(other.recipes)
        items.putAll(other.items)

        updateItemList()
    }

    fun addRecipe(recipeId: Int) {
        val existing = recipes.putIfAbsent(recipeId, RecipeSelection(recipeId))
        if (existing != null) {
            LOG.warn("Recipe $recipeId already present!")
        }

        updateItemList()
    }

    fun removeRecipe(recipeId: Int) {
        if (recipes.remove(recipeId) == null) {
            LOG.warn("Recipe $recipeId wasn't present!")
        }

        updateItemList()
    }

    fun setOreSlotOverride(recipeId: Int, oreDictSlot: Int, itemId: Int) {
        recipes[recipeId]?.also {
            it.slotOverrides[oreDictSlot] = itemId
        } ?: LOG.warn("Tried to set override for recipe $recipeId which isn't present")

        updateItemList()
        solve()
    }

    fun toRecipeGraph(): DefaultDirectedGraph<RecipeSelection, DefaultEdge> {
        val graph = DefaultDirectedGraph<RecipeSelection, DefaultEdge>(DefaultEdge::class.java)

        val recipesByProducts = recipes.values
            .flatMap { recipe -> recipe.recipe.results.map { it.item.id to recipe } }
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } }

        recipes.values.forEach { graph.addVertex(it) }
        recipes.values.forEach { sel ->
            sel.ingredientsProperty
                .get()
                .mapNotNull { it.item.id }
                .flatMap { recipesByProducts[it] ?: emptyList() }
                .toSet()
                .forEach {
                    graph.addEdge(it, sel)
                }
        }

        return graph
    }

    fun reset() {
        recipes.clear()
        items.clear()
        solutionProperty.set(null)
    }

    private fun updateItemList() {
        val existingItems = items.keys.toSet()
        val referencedItems = recipes.values
            .flatMap {
                (it.ingredientsProperty.get() + it.resultsProperty.get())
                    .map { it.item.id }
            }
            .toSet()

        val toRemove = existingItems - referencedItems
        val toAdd = referencedItems - existingItems

        toRemove.forEach { items.remove(it) }
        toAdd.forEach { require(items.putIfAbsent(it, GroupItemAmount(it)) == null) }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RecipeGroup::class.java)
    }
}

private fun <V> Map<Int, V>.forEach(map: MutableMap<Int, GroupItemAmount>, block: (GroupItemAmount, V) -> Unit) {
    forEach { (itemId, amount) ->
        block(map.getValue(itemId), amount)
    }
}
