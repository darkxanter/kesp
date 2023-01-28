package example.database.articles

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.Id
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

@ExposedTable
object ArticleTagsTable : Table("article_tags") {
    @Id
    val articleId = uuid("article_id").references(ArticleTable.id, ReferenceOption.CASCADE)

    @Id
    val tagId = integer("tag_id").references(TagTable.id, ReferenceOption.CASCADE)

    override val primaryKey: PrimaryKey = PrimaryKey(articleId, tagId)
}
