package nec

import nec.model.SchemaImporter
import org.flywaydb.core.Flyway

object BuildDb {
    private val recipeDump = SchemaImporter()

    @JvmStatic
    fun main(args: Array<String>) {
        Flyway.configure().dataSource(
            "jdbc:sqlite:nec.db",
            "", ""
        ).load().apply {
            clean()
            migrate()
        }

        recipeDump.loadFile(args[0])
        timeThis("saveToDb") { recipeDump.saveToDb() }
    }
}
