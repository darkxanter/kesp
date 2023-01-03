package example.database.articles

import com.github.darkxanter.exposed.ksp.annotation.ExposedTable
import com.github.darkxanter.exposed.ksp.annotation.GeneratedValue
import example.database.users.UserTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

@ExposedTable
object ArticleTable : LongIdTable("articles") {
    /** Article title */
    val title = varchar("title", 255)

    /** Article content */
    val content = text("content")

    /** Article author */
    val author = long("author_id").references(UserTable.id)

    @GeneratedValue
    val createdAt = timestamp("created_at")
}
