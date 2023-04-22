package example.dao

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main() {
    Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(UserTable, ArticleTable, CommentTable)

        val userDao = UserTableDao.new {
            username = "test"
        }

        ArticleDao.new {
            this.user = userDao
            this.title = "Test Title"
        }

        ArticleDao.all().forEach {
            println("title '${it.title}' username '${it.user.username}'")
        }
    }
}
