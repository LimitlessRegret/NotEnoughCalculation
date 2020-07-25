package nec.gui

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill

class ItemHighlightManager {
    //    val colorPropertyMap = HashMap<Int, >()
    val backgroundPropertyMap = HashMap<Int, SimpleObjectProperty<Background>>()
    private var currentIndex = 0
    private val colorMap = COLOR_LIST
    private val defaultBackground = Background(emptyArray(), emptyArray())

    fun getBackgroundFor(id: Int): ObservableValue<Background> {
        return backgroundPropertyMap.computeIfAbsent(id) {
            SimpleObjectProperty(defaultBackground)
        }
    }

    fun highlightAll() {
        currentIndex = 0

        backgroundPropertyMap.values.forEach {
            it.set(Background(BackgroundFill(colorMap[currentIndex++ % colorMap.size], null, null)))
        }
    }

    fun highlightNone() {
        backgroundPropertyMap.values.forEach {
            it.set(defaultBackground)
        }
    }

    fun onItemRemoved(itemId: Int) {
        backgroundPropertyMap.remove(itemId)?.set(defaultBackground)
    }

    fun onlyHighlight(itemIds: List<Int>) {
        highlightNone()

        currentIndex = 0
        itemIds
            .mapNotNull { backgroundPropertyMap[it] }
            .forEach {
                it.set(Background(BackgroundFill(FANCY_COLOR_LIST[currentIndex++ % colorMap.size], null, null)))
            }
    }
}
