package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.processor.extensions.isMatched
import com.github.darkxanter.kesp.processor.extensions.panic
import com.github.darkxanter.kesp.processor.generator.model.TableDefinition
import com.github.darkxanter.kesp.processor.helpers.addClass
import com.github.darkxanter.kesp.processor.helpers.addCompanion
import com.github.darkxanter.kesp.processor.helpers.addPrimaryConstructor
import com.github.darkxanter.kesp.processor.helpers.addProperty
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName

//class UserDao(id: EntityID<Long>) : Entity<Long>(id) {
//    companion object : EntityClass<Long, UserDao>(UserTable)
//
//    var username by UserTable.username
//    var password by UserTable.password
//    var birthDate by UserTable.birthDate
//    val createdAt by UserTable.createdAt
//}

internal fun FileSpec.Builder.generateDao(
    tableDefinition: TableDefinition,
//    foreignKeys: List<ForeignKeyDefinition>,
    logger: KSPLogger,
) {
    val tableName = tableDefinition.tableName

    val idTableType = tableDefinition.declaration.getAllSuperTypes().find {
        it.isMatched("org.jetbrains.exposed.dao.id.IdTable")
    } ?: logger.panic("Table $tableName must inherit from IdTable to generate a DAO", tableDefinition.declaration)

    val idType = idTableType.arguments.first().toTypeName()
    val entityId = ClassName("org.jetbrains.exposed.dao.id", "EntityID").parameterizedBy(idType)
    val entityClass = ClassName("org.jetbrains.exposed.dao", "EntityClass")
        .parameterizedBy(idType, tableDefinition.daoClassName)
    val entity = ClassName("org.jetbrains.exposed.dao", "Entity").parameterizedBy(idType)

    addImport("org.jetbrains.exposed.dao", "Entity", "EntityClass")
    addImport("org.jetbrains.exposed.dao.id", "EntityID")

    addClass(tableDefinition.daoClassName) {
        addPrimaryConstructor {
            addParameter("id", entityId)
        }
        superclass(entity)
        addSuperclassConstructorParameter("%L", "id")

        addCompanion {
            superclass(entityClass)
            addSuperclassConstructorParameter("%T", tableDefinition.tableClassName)
        }

        // add own columns
        tableDefinition.allColumns.filter { it.name != "id" }.forEach { column ->
            addProperty(column.name, column.sourceClassName) {
                mutable(!column.generated)
                delegate("%T.%L", tableDefinition.tableClassName, column.name)
            }
        }
        // add references
//        foreignKeys.forEach { foreignKey ->
//            addProperty(foreignKey.sourceColumn.removeSuffix("Id"), foreignKey.daoTypeName) {
//                mutable()
//                delegate(
//                    "%T referencedOn %T.%L",
//                    foreignKey.daoTypeName,
//                    tableDefinition.tableClassName,
//                    foreignKey.targetColumn,
//                )
//            }
//        }
    }
}
