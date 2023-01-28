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
import example.database.users.UserTable
import example.database.users.UserTableCreateDto
import example.database.users.UserTableRepository
import example.database.users.toUserTableFullDtoList
import example.dto.UserDto
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main() {
    Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
//    Database.connect("jdbc:sqlite:./example.sqlite", "org.sqlite.JDBC")

    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    val userRepository = UserTableRepository()
    val articleRepository = ArticleTableRepository()
    val articleTagRepository = ArticleTagsTableRepository()
    val tagRepository = TagTableRepository()

    val dto = UserTableCreateDto(
        "test",
        "pass",
    )

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.createMissingTablesAndColumns(
            UserTable,
            ArticleTable,
            ArticleTagsTable,
            TagTable,
        )
        printDivider()

        val userId = userRepository.create(dto)

        userRepository.updateUserDto(userId, UserDto(userId, "another"))

        val tagId = tagRepository.create(TagTableCreateDto("example"))
        tagRepository.update(tagId, TagTableCreateDto("Example"))

        val articleId = articleRepository.create(ArticleTableCreateDto(
            "Test Title",
            "Some content",
            userId,
        ))
        articleTagRepository.create(ArticleTagsTableCreateDto(articleId, tagId))

        printDivider()
        println(userRepository.find())
        printDivider()
        println(userRepository.findUserDto())
        printDivider()
        println(articleRepository.find())
        printDivider()
        println(articleTagRepository.find())
        printDivider()
        println(tagRepository.find())

        val userAlias = UserTable.alias("user_alias")
        printDivider()
        println(userAlias.selectAll().toUserTableFullDtoList(userAlias))
        printDivider()

        articleRepository.deleteById(articleId)
        tagRepository.deleteById(tagId)
        userRepository.deleteById(userId)
    }
}

fun printDivider() = println("=".repeat(20))
