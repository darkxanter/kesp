package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.processor.extensions.isMatched
import com.github.darkxanter.kesp.processor.extensions.panic
import com.github.darkxanter.kesp.processor.generator.model.ColumnDefinition
import com.github.darkxanter.kesp.processor.generator.model.TableDefinition
import com.github.darkxanter.kesp.processor.helpers.CallableParam
import com.github.darkxanter.kesp.processor.helpers.addCall
import com.github.darkxanter.kesp.processor.helpers.addClass
import com.github.darkxanter.kesp.processor.helpers.addCodeBlock
import com.github.darkxanter.kesp.processor.helpers.addCompanion
import com.github.darkxanter.kesp.processor.helpers.addFunction
import com.github.darkxanter.kesp.processor.helpers.addPrimaryConstructor
import com.github.darkxanter.kesp.processor.helpers.addProperty
import com.github.darkxanter.kesp.processor.helpers.addReturn
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
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
    addImport("org.jetbrains.exposed.dao", "DaoEntityID")

    addClass(tableDefinition.daoBaseClassName) {
        modifiers.add(KModifier.ABSTRACT)

        addPrimaryConstructor {
            addParameter("id", entityId)
        }
        superclass(entity)
        addSuperclassConstructorParameter("%L", "id")

        // add own columns
        tableDefinition.allColumns.filter { it.name != "id" }.forEach { column ->
            addProperty(column.name, column.sourceClassName) {
                mutable(!column.generated)
                delegate("%T.%L", tableDefinition.tableClassName, column.name)
            }
        }
    }

    addClass(tableDefinition.daoClassName) {
        addPrimaryConstructor {
            addParameter("id", entityId)
        }
        superclass(tableDefinition.daoBaseClassName)
        addSuperclassConstructorParameter("%L", "id")

        addCompanion {
            superclass(entityClass)
            addSuperclassConstructorParameter("%T", tableDefinition.tableClassName)
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

    generateMappings(tableDefinition, logger)
}

private fun FileSpec.Builder.generateMappings(tableDefinition: TableDefinition, logger: KSPLogger) {

    generateWriteMappings(tableDefinition, logger)

    generateReadMappings(
        dtoClassName = tableDefinition.fullDtoClassName,
        columns = tableDefinition.allColumns,
        tableDefinition = tableDefinition,
    )

    tableDefinition.projections.filter { it.readFunction }.forEach { projection ->
        generateReadMappings(
            dtoClassName = projection.className,
            columns = projection.columns,
            tableDefinition = tableDefinition,
        )
    }
}

private fun FileSpec.Builder.generateWriteMappings(
    tableDefinition: TableDefinition,
    logger: KSPLogger,
) {
    addFunction(MAPPING_FROM_FUN_NAME) {
        receiver(tableDefinition.daoBaseClassName)
        addParameter("dto", tableDefinition.createInterfaceClassName)

        tableDefinition.commonColumns.forEach { column ->
            val name = column.name
            if (column.isEntityId) {
                val foreignTable = column.foreignTable ?: logger.error(
                    "Column $name must be annotated with @ForeignKey",
                    tableDefinition.declaration,
                )
                addStatement("this.$name = DaoEntityID(dto.$name, %L)", foreignTable)
            } else {
                addStatement("this.$name = dto.$name")
            }
        }
    }
}

private fun FileSpec.Builder.generateReadMappings(
    dtoClassName: ClassName,
    columns: List<ColumnDefinition>,
    tableDefinition: TableDefinition,
) {
    val receiver = tableDefinition.daoBaseClassName

    val functionName = "to${dtoClassName.simpleName}"
    val functionListName = "${functionName}List"

    addFunction(functionName) {
        receiver(receiver)
        returns(dtoClassName)
        addCodeBlock {
            addReturn()
            addCall(dtoClassName.simpleName, columns) { column ->
                val name = column.name
                CallableParam(name, "$name${column.unwrapEntityId}")
            }
        }
    }

    addFunction(functionListName) {
        receiver(Iterable::class.asClassName().parameterizedBy(receiver))
        returns(List::class.asClassName().parameterizedBy(dtoClassName))
        addCodeBlock {
            addReturn()
            beginControlFlow("map")
            addStatement("it.${functionName}()")
            endControlFlow()
        }
    }
}
