package nec.gui


import javafx.stage.Stage
import tornadofx.App
import tornadofx.find
import tornadofx.launch
import java.io.File

class NecApp : App(MasterView::class, Styles::class) {
    override fun start(stage: Stage) {
        super.start(stage)

        val rgFile = parameters.named["load"]
        if (rgFile != null) {
            find<RecipeCalculationViewModel>(scope).load(File(rgFile))
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch<NecApp>(args)
        }
    }
}