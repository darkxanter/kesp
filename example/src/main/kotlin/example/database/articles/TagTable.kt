package example.database.articles

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.GeneratedValue
import com.github.darkxanter.kesp.annotation.Id
import org.jetbrains.exposed.dao.id.IdTable

@ExposedTable
object TagTable : IdTable<Int>("tags") {
    @Id
    @GeneratedValue
    override val id = integer("id").autoIncrement().entityId()

    /** Tag label */
    val label = varchar("title", 255)


    override val primaryKey = PrimaryKey(id)
}
