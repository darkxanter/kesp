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

@ExposedTable
@Projection(UserDto::class)
object UserTable : LongIdTable("users") {
    /**
     * Username
     */
    val username = varchar("username", 255)

    /**
     * password
     */
    val password = varchar("password", 255)

    val birthDate = date("birth_date").nullable()

    val profile = json<UserProfile>("profile")

    @GeneratedValue
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}


@Serializable
data class UserProfile(
    val value1: String,
    val value2: Int,
)
