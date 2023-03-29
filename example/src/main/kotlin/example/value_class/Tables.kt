package example.value_class

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.GeneratedValue
import example.value_class.CommentTable.text
import org.jetbrains.exposed.dao.id.IdTable

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
