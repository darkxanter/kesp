package example.dao

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ArticleDao(id: EntityID<Int>) : ArticleTableDaoBase(id) {
    companion object : EntityClass<Int, ArticleDao>(ArticleTable)

    var user by UserTableDao referencedOn ArticleTable.userId
}

