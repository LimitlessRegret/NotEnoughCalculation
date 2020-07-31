package nec

import nec.model.SchemaImporter

object BuildDb {
    private val recipeDump = SchemaImporter()

    @JvmStatic
    fun main(args: Array<String>) {
        recipeDump.loadFile(args[0])
        timeThis("saveToDb") { recipeDump.saveToDb() }
    }
}
