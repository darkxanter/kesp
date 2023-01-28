package example.database.articles

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.GeneratedValue
import com.github.darkxanter.kesp.annotation.Projection
import example.database.users.UserTable
import example.dto.ArticleTitleDto
import example.dto.ArticleTitleWithAuthorDto
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

@ExposedTable
@Projection(ArticleTitleDto::class)
@Projection(ArticleTitleWithAuthorDto::class)
object ArticleTable : UUIDTable("articles") {
    /** Article title */
    val title = varchar("title", 255)

    /** Article content */
    val content = text("content")

    /** Article author */
    val authorId = long("author_id").references(UserTable.id, ReferenceOption.CASCADE)

    @GeneratedValue
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

