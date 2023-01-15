package example.database.articles

import com.github.darkxanter.exposed.ksp.annotation.ExposedTable
import com.github.darkxanter.exposed.ksp.annotation.GeneratedValue
import com.github.darkxanter.exposed.ksp.annotation.Id
import example.database.users.UserTable
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

@ExposedTable
object ArticleTable : UUIDTable("articles") {
    /** Article title */
    val title = varchar("title", 255)

    /** Article content */
    val content = text("content")

    /** Article author */
    val author = long("author_id").references(UserTable.id)

    @GeneratedValue
    val createdAt = timestamp("created_at")
}

@ExposedTable
object TagTable : IdTable<Int>("tags") {
    @Id
    @GeneratedValue
    override val id = integer("id").autoIncrement().entityId()

    /** Tag label */
    val label = varchar("title", 255)
}

@ExposedTable
object ArticleTagsTable : Table("article_tags") {
    @Id
    val article = uuid("article").references(ArticleTable.id)

    @Id
    val tag = integer("tag").references(TagTable.id)
}
