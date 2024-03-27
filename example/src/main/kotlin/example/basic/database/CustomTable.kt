package example.basic.database

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.Id
import org.jetbrains.exposed.sql.Table

@ExposedTable
object CustomIdTable : Table("custom_id") {
    @Id
    val uuid = uuid("uuid")
    val flag = bool("flag")
}
