package example

import example.database.articles.ArticleTable
import example.database.articles.ArticleTableCreateDto
import example.database.articles.ArticleTableRepository
import example.database.articles.ArticleTagsTable
import example.database.articles.ArticleTagsTableCreateDto
import example.database.articles.ArticleTagsTableRepository
import example.database.articles.TagTable
import example.database.articles.TagTableCreateDto
import example.database.articles.TagTableRepository
import example.database.users.UserProfile
import example.database.users.UserTable
import example.database.users.UserTableCreateDto
import example.database.users.UserTableRepository
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

    val userRepository = UserTableRepository()
    val articleRepository = ArticleTableRepository()
    val articleTagRepository = ArticleTagsTableRepository()
    val tagRepository = TagTableRepository()

    val dto = UserTableCreateDto(
        "test",
        "pass",
        profile = UserProfile("answer", 42),
    )

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.createMissingTablesAndColumns(
            UserTable,
            ArticleTable,
            ArticleTagsTable,
            TagTable,
        )

        val userId = userRepository.create(dto)

        val tagId = tagRepository.create(TagTableCreateDto("example"))
        tagRepository.update(tagId, TagTableCreateDto("Example"))

        val articleId = articleRepository.create(ArticleTableCreateDto(
            "Test Title",
            "Some content",
            userId,
        ))
        articleTagRepository.create(ArticleTagsTableCreateDto(articleId, tagId))

        println(userRepository.find())
        println(articleRepository.find())
        println(articleTagRepository.find())
        println(tagRepository.find())


        articleRepository.deleteById(articleId)
        tagRepository.deleteById(tagId)
        userRepository.deleteById(userId)
    }
}
