package nec.gui

import freetimelabs.io.reactorfx.flux.FxFlux
import javafx.beans.value.ObservableValue
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.RadioButton
import javafx.scene.control.TableCell
import javafx.scene.control.TableRow
import javafx.scene.input.MouseEvent
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
    toggleClass(Stylesheet.tableCell, true)
    toggleClass(Stylesheet.tableColumn, true)
    toggleClass(Stylesheet.textField, true)
}