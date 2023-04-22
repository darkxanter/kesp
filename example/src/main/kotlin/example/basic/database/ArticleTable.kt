package example.basic.database

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.GeneratedValue
import com.github.darkxanter.kesp.annotation.Projection
import example.basic.dto.ArticleTitleDto
import example.basic.dto.ArticleTitleWithAuthorDto
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
    val authorId = reference("author_id", UserTable.id, ReferenceOption.CASCADE).nullable()

    @GeneratedValue
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

