package example.table_class

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main() {
    Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
//    Database.connect("jdbc:postgresql://localhost:5432/test", driver = "org.postgresql.Driver", user = "postgres")

    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        addLogger(StdOutSqlLogger)
        val table = ArticleTable()
        val repository = ArticleTableRepository(table)

        SchemaUtils.drop(table)
        SchemaUtils.create(table)
        val articleId = table.insertDto(ArticleTableCreateDto("Test article"))

        println(table.selectAll().where { table.id eq articleId }.toList())
        println(repository.find())
    }
}
