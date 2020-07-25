package nec.gui.recipe

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import nec.RecipeDatabase
import nec.dbmodel.DbRecipe
import nec.gui.onChangeDebounce
import nec.gui.recipe.ItemSearchMode.*
import nec.timeThis
import tornadofx.*

data class MachineRecipeGroup(
    val machine: String,
    val recipes: List<DbRecipe>
)

class RecipeSearchViewModel : ViewModel() {
    private val recipeDump = RecipeDatabase.instance // TODO
    val machineList = arrayListOf<MachineRecipeGroup>().asObservable()

    val itemQueryProperty = SimpleStringProperty("")
    private val itemQuery by itemQueryProperty
    private val searchModeProperty = SimpleObjectProperty(ANY)
    var searchMode: ItemSearchMode by searchModeProperty
    val selectedMachineProperty = SimpleObjectProperty<MachineRecipeGroup>(null)
    private var selectedMachine: MachineRecipeGroup? by selectedMachineProperty
    private var lastUserSelectedMachine: String? = null
    val selectedRecipeIndexProperty = SimpleIntegerProperty(-1)
    private var selectedRecipeIndex by selectedRecipeIndexProperty
    val selectedRecipeProperty = selectedRecipeIndexProperty.objectBinding(selectedMachineProperty) {
//        println("srp update - it=$it, sm=$selectedMachine")
        if (it !is Int || it < 0) null else selectedMachine?.recipes?.getOrNull(it)
    }
    private val selectedRecipe by selectedRecipeProperty
    val currentMachineRecipeCountProperty = selectedMachineProperty.integerBinding {
        it?.recipes?.size ?: 0
    }
    val currentMachineRecipeCount by currentMachineRecipeCountProperty
    val machineHasNextRecipeProperty =
        selectedRecipeIndexProperty.booleanBinding(currentMachineRecipeCountProperty) {
            (it as Int + 1) in 0 until currentMachineRecipeCount
        }
    val machineHasPreviousRecipeProperty =
        selectedRecipeIndexProperty.booleanBinding(currentMachineRecipeCountProperty) {
            (it as Int - 1) in 0 until currentMachineRecipeCount
        }
    val statusTextProperty = SimpleStringProperty()
    private var statusText by statusTextProperty

    init {
        itemQueryProperty.onChangeDebounce(150) {
            refreshSearch()
        }
        searchModeProperty.onChangeDebounce(150) {
            refreshSearch()
        }
        selectedMachineProperty.onChange {
            if (it?.machine != null) {
                lastUserSelectedMachine = it.machine

                selectedRecipeIndex = 0
            } else {
                selectedRecipeIndex = -1
            }
        }
    }

    private fun refreshSearch() = timeThis("refreshSearch()") {
        if (itemQuery.length < 3) {
            statusText = "Item query too short!"
            return@timeThis
        }

        statusText = "Searching items"
        val items = if (itemQuery.startsWith("item:")) {
            val itemId = itemQuery.split(':')[1].toIntOrNull()
            itemId?.let { listOf(it) } ?: emptyList()
        } else {
            timeThis("search items") { recipeDump.lookupByLocalizedName(itemQuery) }
        }
        println("Found ${items.size} items matching query")

        statusText = "Searching recipes for ${items.size} items"
        val recipes = timeThis("search recipes") {
            when (searchMode) {
                INPUT_ONLY -> recipeDump.findRecipeByIngredient(items)
                OUTPUT_ONLY -> recipeDump.findRecipeByResult(items)
                ANY -> recipeDump.findRecipeByIngredient(items).union(recipeDump.findRecipeByResult(items))
            }
        }
        println("Found ${recipes.size} recipes with item in $searchMode mode")

        val machineGroups = timeThis("Group into machines") {
            recipes
                .groupBy { it.machine }
                .mapValues { MachineRecipeGroup(it.key, it.value) }
        }

        runLater {
            machineList.clear()
            machineList.addAll(machineGroups.values.sortedBy { it.machine })

            if (lastUserSelectedMachine != null && machineGroups.containsKey(lastUserSelectedMachine)) {
                selectedMachineProperty.set(machineGroups.getValue(lastUserSelectedMachine!!))
            }

            statusText = "Done, ${recipes.size} recipes"
        }
    }

    fun selectRecipe(delta: Int, wrap: Boolean = false) {
        selectedRecipeIndex = (selectedRecipeIndex + delta).let {
            when {
                it < 0 -> if (wrap) currentMachineRecipeCount - 1 else selectedRecipeIndex
                it >= currentMachineRecipeCount -> if (wrap) 0 else selectedRecipeIndex
                else -> it
            }
        }
    }

    fun selectMachine(delta: Int) {
        val newValue = machineList.indexOf(selectedMachine) + delta
        if (newValue !in 0 until machineList.size) {
            return
        }

        selectedMachine = machineList[newValue]
    }
}

enum class ItemSearchMode {
    INPUT_ONLY,
    OUTPUT_ONLY,
    ANY
}