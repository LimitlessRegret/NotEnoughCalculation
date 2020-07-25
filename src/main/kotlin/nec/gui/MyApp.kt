package nec.gui


import tornadofx.App
import tornadofx.launch

class MyApp : App(MasterView::class, Styles::class) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch<MyApp>(args)
        }
    }
}