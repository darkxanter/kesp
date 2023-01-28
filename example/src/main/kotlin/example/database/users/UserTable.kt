package example.database.users

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.GeneratedValue
import com.github.darkxanter.kesp.annotation.Projection
import example.database.json
import example.dto.UserDto
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

/** User account */
@ExposedTable
@Projection(UserDto::class, updateFunction = true)
object UserTable : LongIdTable("users") {
    /**
     * Username
     */
    val username = varchar("username", 255)

    /** User password */
    val password = varchar("password", 255)

    /** Day of birth */
    val birthDate = date("birth_date").nullable()

    /** Account creation time */
    @GeneratedValue
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}
