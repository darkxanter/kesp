package example.value_class

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.Id
import org.jetbrains.exposed.sql.Table

@JvmInline
value class ArticleId(override val value: Int) : ValueId<Int>

@ExposedTable
object ArticleTable : ValueIntIdTable<ArticleId>("articles", ::ArticleId, ArticleId::class) {
    val title = text("title")
}

@JvmInline
value class CommentId(override val value: Int) : ValueId<Int>

@ExposedTable
object CommentTable : ValueIntIdTable<CommentId>("comments", ::CommentId, CommentId::class) {
    val articleId = reference("article_id", ArticleTable.id)
    val message = text("message")
}


@ExposedTable
object TableWithCustomId : Table("table_with_custom_id") {
    @Id
    val articleId = reference("article_id", ArticleTable.id)
    val description = text("description")
}
