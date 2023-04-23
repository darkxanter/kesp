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
    /** Annotation [ForeignKey] is not required if column type is simple `Column<Int>` */
    val articleId = integer("article_id").references(ArticleTable.id)
    val message = text("message")

    /** Annotation [ForeignKey] is required if column type is `Column<Entity<*>>` */
    @ForeignKey(UserTable::class)
    val userId = reference("user_id", UserTable)
}

@ExposedTable(generateDao = true)
object UserTable : IntIdTable("users") {
    val username = text("username")
}
