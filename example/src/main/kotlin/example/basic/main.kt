package example.basic

import example.basic.database.ArticleTable
import example.basic.database.ArticleTableCreateDto
import example.basic.database.ArticleTableRepository
import example.basic.database.ArticleTagsTable
import example.basic.database.ArticleTagsTableCreateDto
import example.basic.database.ArticleTagsTableRepository
import example.basic.database.TagTable
import example.basic.database.TagTableCreateDto
import example.basic.database.TagTableRepository
import example.basic.database.UserTable
import example.basic.database.UserTableCreateDto
import example.basic.database.UserTableRepository
import example.basic.database.toUserTableFullDtoList
import example.basic.dto.UserDto
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

        userRepository.updateIUser(userId, UserDto(userId, "another"))

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
        println(userRepository.find({ table ->
            orderBy(table.id)
        }) { table ->
            table.id eq userId
        })
        printDivider()
        println(userRepository.findUserDto())
        printDivider()
        println(articleRepository.find())
        println(articleRepository.find {
            it.authorId eq userId
        })
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
