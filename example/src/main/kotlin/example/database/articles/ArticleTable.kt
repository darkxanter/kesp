package example.database.articles

import com.github.darkxanter.exposed.ksp.annotation.ExposedTable
import com.github.darkxanter.exposed.ksp.annotation.GeneratedValue
import com.github.darkxanter.exposed.ksp.annotation.Id
import example.database.users.UserTable
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

@ExposedTable
object ArticleTable : UUIDTable("articles") {
    /** Article title */
    val title = varchar("title", 255)

    /** Article content */
    val content = text("content")

    /** Article author */
    val author = long("author_id").references(UserTable.id, ReferenceOption.CASCADE)

    @GeneratedValue
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

@ExposedTable
object TagTable : IdTable<Int>("tags") {
    @Id
    @GeneratedValue
    override val id = integer("id").autoIncrement().entityId()

    /** Tag label */
    val label = varchar("title", 255)


    override val primaryKey = PrimaryKey(id)
}

@ExposedTable
object ArticleTagsTable : Table("article_tags") {
    @Id
    val article = uuid("article").references(ArticleTable.id, ReferenceOption.CASCADE)

    @Id
    val tag = integer("tag").references(TagTable.id, ReferenceOption.CASCADE)

    override val primaryKey: PrimaryKey = PrimaryKey(article, tag)
}
