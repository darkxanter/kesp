package example.value_class

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main() {
    Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
//    Database.connect("jdbc:postgresql://localhost:5432/test", driver = "org.postgresql.Driver", user = "postgres")

    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.drop(ArticleTable, CommentTable)
        SchemaUtils.create(ArticleTable, CommentTable)
        val articleId = ArticleTable.insertDto(ArticleTableCreateDto("Test article"))
        val commentId = CommentTable.insertDto(CommentTableCreateDto(
            articleId = articleId,
            message = "test message"
        ))

        println(ArticleTable.select { ArticleTable.id eq articleId }.toList())
        println(CommentTable.select { CommentTable.id eq commentId }.toList())
        println(CommentTable.select { CommentTable.articleId eq articleId }.toList())
    }
}
