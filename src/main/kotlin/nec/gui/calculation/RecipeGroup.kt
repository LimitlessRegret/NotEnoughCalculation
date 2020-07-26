package nec.gui.calculation

import javafx.beans.property.SimpleObjectProperty
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import nec.dbmodel.DbRecipe
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
    val recipes = HashMap<Int, RecipeSelection>().asObservable()
    val items = HashMap<Int, GroupItemAmount>().asObservable()
    val solutionProperty = SimpleObjectProperty<RecipeSolverSolution>()
    private var solution by solutionProperty

    fun solve() {
        if (recipes.isEmpty()) return

        val wrapper = RecipeMPSolverWrapper.from(this)
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

        val toRemove = items.values.filter { it.isZero }
        println("toRemove= $toRemove")
    }

    fun save(file: File) = file.writeText(save())
    fun save() = Json(JsonConfiguration.Stable).stringify(serializer(), this)
    fun load(file: File) = load(file.readText())
    fun load(data: String) = load(Json(JsonConfiguration.Stable).parse(serializer(), data))
    fun load(other: RecipeGroup) {
        items.clear()
        recipes.clear()
        recipes.putAll(other.recipes)
        items.putAll(other.items)
    }

    fun addRecipe(recipeId: Int) {
        val existing = recipes.putIfAbsent(recipeId, RecipeSelection(recipeId))
        if (existing != null) {
            LOG.warn("Recipe $recipeId already present!")
        }
    }

    fun removeRecipe(recipeId: Int) {
        if (recipes.remove(recipeId) == null) {
            LOG.warn("Recipe $recipeId wasn't present!")
        }
    }

    fun toRecipeGraph(): DefaultDirectedGraph<DbRecipe, DefaultEdge> {
        val graph = DefaultDirectedGraph<DbRecipe, DefaultEdge>(DefaultEdge::class.java)

        val recipesByProducts = recipes.values
            .flatMap { recipe -> recipe.recipe.results.map { it.item?.id to recipe.recipe } }
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } }

        recipes.values.forEach { graph.addVertex(it.recipe) }
        recipes.values.forEach { sel ->
            sel.recipe.ingredients
                .mapNotNull { it.item?.id }
                .flatMap { recipesByProducts[it] ?: emptyList() }
                .toSet()
                .forEach {
                    graph.addEdge(it, sel.recipe)
                }
        }

        return graph
    }

    @Serializer(forClass = RecipeGroup::class)
    companion object : KSerializer<RecipeGroup> {
        private val LOG = LoggerFactory.getLogger(RecipeGroup::class.java)

        override val descriptor: SerialDescriptor = SerialDescriptor("RecipeGroup") {
            element<Map<Int, RecipeSelection>>("recipes")
            element<Map<Int, GroupItemAmount>>("items")
        }

        override fun serialize(encoder: Encoder, value: RecipeGroup) {
            val compositeOutput = encoder.beginStructure(descriptor)
            compositeOutput.encodeSerializableElement(
                descriptor,
                0,
                MapSerializer(Int.serializer(), RecipeSelection.serializer()),
                value.recipes
            )
            compositeOutput.encodeSerializableElement(
                descriptor,
                1,
                MapSerializer(Int.serializer(), GroupItemAmount.serializer()),
                value.items
            )
            compositeOutput.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): RecipeGroup {
            val rg = RecipeGroup()
            val dec: CompositeDecoder = decoder.beginStructure(descriptor)
            loop@ while (true) {
                when (val i = dec.decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_DONE -> break@loop
                    0 -> rg.recipes.putAll(
                        dec.decodeSerializableElement(
                            descriptor,
                            i,
                            MapSerializer(Int.serializer(), RecipeSelection.serializer())
                        )
                    )
                    1 -> rg.items.putAll(
                        dec.decodeSerializableElement(
                            descriptor,
                            i,
                            MapSerializer(Int.serializer(), GroupItemAmount.serializer())
                        )
                    )
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            dec.endStructure(descriptor)
            return rg
        }
    }
}

private fun <V> Map<Int, V>.forEach(map: MutableMap<Int, GroupItemAmount>, block: (GroupItemAmount, V) -> Unit) {
    forEach { (itemId, amount) ->
        val gia = map.computeIfAbsent(itemId) { GroupItemAmount(itemId) }
        block(gia, amount)
    }
}
