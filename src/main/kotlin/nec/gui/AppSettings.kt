package nec.gui

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.Controller
import tornadofx.getValue
import tornadofx.setValue

class AppSettings : Controller() {
    val showInfiniteCostsProperty = SimpleBooleanProperty(false)
    var showInfiniteCosts by showInfiniteCostsProperty
    val showInternalIdsProperty = SimpleBooleanProperty(false)
    var showInternalIds by showInternalIdsProperty
    val showTestBarProperty = SimpleBooleanProperty(false)
    var showTestBar by showTestBarProperty
}