package nec.gui

import com.sun.javafx.scene.control.skin.ListViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import freetimelabs.io.reactorfx.flux.FxFlux
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.control.RadioButton
import javafx.scene.control.TableCell
import javafx.scene.control.TableRow
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tornadofx.*
import java.time.Duration


fun RadioButton.activateOn(ctx: UIComponent, combo: String) {
    ctx.shortcut(combo) {
        isSelected = true
        onAction.handle(ActionEvent())
    }
}

fun <T> ObservableValue<T>.onChangeDebounce(window: Long, block: (T) -> Unit) {
    FxFlux.from(this)
        .onBackpressureLatest()
        .sampleTimeout { Mono.delay(Duration.ofMillis(window)) }
        .publishOn(Schedulers.single())
        .subscribe(block)
}

fun Double.toIntLikeString(): String {
    if (this == toInt().toDouble()) return "${toInt()}"

    return toString()
}

fun <K, V> ObservableMap<K, V>.bindValuesToList(list: ObservableList<V>) {
    addListener(MapChangeListener {
//        println("mcl ${it.valueAdded} ${it.valueRemoved} ${it.key}")
        when {
            it.valueAdded != null -> list.add(it.valueAdded)
            it.valueRemoved != null -> list.remove(it.valueRemoved)
        }
    })
}

inline fun <reified T> MouseEvent.findItem(): T? {
    val cell = target as? TableCell<*, T?>
        ?: (target as? Node)?.findParent()
    val row = target as? TableRow<T?>
        ?: (target as? Node)?.findParent()

    return when {
        cell?.item is T -> cell?.item
        row?.item is T -> row?.item
        else -> null
    }
}

fun TableCell<*, *>.setDefaultTableCellStyles() {
    // Why do these sometimes disappear?
    toggleClass(Stylesheet.cell, true)
    toggleClass(Stylesheet.indexedCell, true)
    removeClass(Stylesheet.tableCell)
    toggleClass(Styles.itemCell, true)
    toggleClass(Stylesheet.tableColumn, true)
    toggleClass(Stylesheet.textField, true)
}

fun <T> ListView<T>.scrollSelectionWithWheel(selectionProperty: SimpleObjectProperty<T>) {
    addEventFilter(ScrollEvent.SCROLL) {
        it.consume()

        val newIndex = selectionModel.selectedIndex + if (it.deltaY < 0) 1 else -1

        if (newIndex !in 0 until items.size) {
            return@addEventFilter
        }

        selectionProperty.set(items[newIndex])
    }

    keepSelectedInView(selectionProperty)
}

fun <T> ListView<T>.keepSelectedInView(selectionProperty: SimpleObjectProperty<T>) {
    bindSelected(selectionProperty)

    selectionProperty.onChange {
        val ts = skin as ListViewSkin<*>
        val vf = ts.children[0] as VirtualFlow<*>

        val priorSelection = selectionModel.selectedIndices.firstOrNull() ?: 0
        val itemIndex = items.indexOf(it)

        val scrollBufferSpace = 3
        if (itemIndex < scrollBufferSpace || itemIndex >= items.size - scrollBufferSpace) {
            /* Do nothing */
        } else if (itemIndex > priorSelection) {
            vf.show(itemIndex + scrollBufferSpace)
        } else {
            vf.show(itemIndex - scrollBufferSpace)
        }

        selectionModel.select(it)
    }
}