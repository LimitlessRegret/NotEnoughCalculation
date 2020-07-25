package nec.gui

import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.shape.StrokeType
import javafx.scene.text.FontWeight
import tornadofx.*

class Styles : Stylesheet() {
    companion object {
        // Define our styles
        val itemRawInput by cssclass()
        val itemOutput by cssclass()
        val itemCell by cssclass()

        // Define our colors
        val dangerColor = c("#a94442")
        val hoverColor = c("#d49942")
    }

    init {
        itemCell {
            padding = box(0.166667.em)
            cellSize = 2.em
        }
        itemRawInput {
            fontWeight = FontWeight.BOLD
        }
        itemOutput {
            borderColor += box(dangerColor)
            borderStyle += BorderStrokeStyle(
                StrokeType.INSIDE,
                StrokeLineJoin.MITER,
                StrokeLineCap.BUTT,
                10.0,
                0.0,
                listOf(25.0, 5.0)
            )
            borderWidth += box(1.px)
        }
    }
}