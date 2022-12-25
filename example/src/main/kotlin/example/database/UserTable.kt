package example.database

import com.github.darkxanter.exposed.annotation.ExposedTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

@ExposedTable
object UserTable : LongIdTable("users") {
    /**
     * Username
     */
    val username = varchar("username", 255)

    /**
     * password
     */
    val password = varchar("password", 255)

    val birthDate = datetime("birth_date").nullable()
}
