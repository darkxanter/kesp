package example

import example.database.users.UserProfile
import example.database.users.UserTable
import example.database.users.UserTableCreateDto
import example.database.users.UserTableRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main() {
    val json = Json {
        serializersModule = SerializersModule {
            contextual(InstantSerializer)
        }
    }

    Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    val repository = UserTableRepository()

    val dto = UserTableCreateDto(
        "test",
        "pass",
        profile = UserProfile("answer", 42),
    )

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.createMissingTablesAndColumns(UserTable)
        val id = repository.create(dto)
        println("id = $id")

        val entity = repository.findById(id)
        println(entity)
        println(json.encodeToString(entity))
        repository.update(id, dto.copy(username = "user"))
        println(repository.findById(id))
        repository.deleteById(id)
        println(repository.findById(id))
    }
}
