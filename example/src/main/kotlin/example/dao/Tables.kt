package example.dao

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.ForeignKey
import org.jetbrains.exposed.dao.id.IntIdTable

@ExposedTable(generateDao = true)
object ArticleTable : IntIdTable() {
    val title = text("title")

    @ForeignKey(UserTable::class)
    val userId = reference("user_id", UserTable)
}

@ExposedTable(generateDao = true)
object CommentTable : IntIdTable() {
    @ForeignKey(ArticleTable::class)
    val articleId = reference("article_id", ArticleTable)
    val message = text("message")

    @ForeignKey(UserTable::class)
    val userId = reference("user_id", UserTable)
}

@ExposedTable(generateDao = true)
object UserTable : IntIdTable("users") {
    val username = text("username")
}
