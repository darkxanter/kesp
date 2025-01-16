package example.table_class

import com.github.darkxanter.kesp.annotation.ExposedTable
import org.jetbrains.exposed.dao.id.LongIdTable

@ExposedTable
class ArticleTable : LongIdTable("articles") {
    val title = text("title")
}
