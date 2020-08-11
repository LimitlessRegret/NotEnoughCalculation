package nec.gui.oredict

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import nec.RecipeDatabase
import nec.dbmodel.tables.pojos.Item
import nec.gui.onChangeDebounce
import nec.timeThis
import tornadofx.*

data class OreDictItemGroup(
    val dictName: String,
    val items: Array<Item>
)

class OreDictChoiceViewModel : ViewModel() {
    private val recipeDump = RecipeDatabase.instance // TODO
    private val rawOreDictItems = arrayListOf<OreDictItemGroup>().asObservable()
    private val rawOreDictItemIdsProperty = SimpleObjectProperty<Set<Int>>(emptySet())
    private var rawOreDictItemIds by rawOreDictItemIdsProperty
    val itemsInDicts = arrayListOf<OreDictItemGroup>().asObservable()

    val forRecipeIdProperty = SimpleIntegerProperty()
    val forOreSlotProperty = SimpleIntegerProperty()
    val oreDictsProperty = SimpleObjectProperty<List<Int>>(emptyList())
    private val oreDicts by oreDictsProperty
    val itemQueryProperty = SimpleStringProperty("")
    private val itemQuery by itemQueryProperty
    val selectedOreDictGroupProperty = SimpleObjectProperty<OreDictItemGroup>(null)
    private var selectedOreDictGroup: OreDictItemGroup? by selectedOreDictGroupProperty
    val selectedItemProperty = SimpleObjectProperty<Item>(null)
    private var selectedItem: Item? by selectedItemProperty
    val statusTextProperty = SimpleStringProperty()
    private var statusText by statusTextProperty

    init {
        oreDictsProperty.onChangeDebounce(50) {
            refresh()
        }
        itemQueryProperty.onChangeDebounce(50) {
            refresh()
        }
        rawOreDictItems.onChange {
            rawOreDictItemIds = rawOreDictItems.flatMap {
                it.items.map { it.id }
            }.toSet()
        }
    }

    private fun refresh() = timeThis("refresh()", debug = true) {
        val items = timeThis("getOreDicts", debug = true) { recipeDump.getOreDicts(oreDicts, true) }

        rawOreDictItems.setAll(
            items.map {
                OreDictItemGroup(it.name, it.items)
            }
        )

        if (itemQuery.isEmpty()) {
            runLater { itemsInDicts.setAll(rawOreDictItems) }
            return@timeThis
        }

        statusText = "Searching items"
        val itemIds = if (itemQuery.startsWith("item:")) {
            val itemId = itemQuery.split(':')[1].toIntOrNull()
            if (itemId != null && itemId in rawOreDictItemIds) {
                setOf(itemId)
            } else emptySet()
        } else {
            timeThis("search items", debug = true) {
                recipeDump.lookupByLocalizedName(itemQuery, rawOreDictItemIds).toSet()
            }
        }

        val filteredItems = timeThis("Filter items", debug = true) {
            rawOreDictItems
                .map { OreDictItemGroup(it.dictName, it.items.filter { it.id in itemIds }.toTypedArray()) }
                .filter { it.items.isNotEmpty() }
        }

        runLater {
            itemsInDicts.setAll(filteredItems)

            statusText = "Done, ${itemIds.size} items"
        }
    }

    fun selectOreDict(delta: Int) {
        val newValue = itemsInDicts.indexOf(selectedOreDictGroup) + delta
        if (newValue !in 0 until itemsInDicts.size) {
            return
        }

        selectedOreDictGroup = itemsInDicts[newValue]
    }

    fun formatItemName(item: Item?): String {
        if (item == null) return ""

        val modSuffix = recipeDump.modIdCache[item.modId]?.let {
            if (it == "unknown") ""
            else " ($it)"
        } ?: ""

        return "${item.localizedName}$modSuffix"
    }
}
