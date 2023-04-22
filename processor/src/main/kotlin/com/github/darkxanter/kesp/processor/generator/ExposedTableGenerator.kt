package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.GeneratedValue
import com.github.darkxanter.kesp.annotation.Id
import com.github.darkxanter.kesp.annotation.Projection
import com.github.darkxanter.kesp.processor.Configuration
import com.github.darkxanter.kesp.processor.extensions.filterAnnotations
import com.github.darkxanter.kesp.processor.extensions.getFirstArgumentType
import com.github.darkxanter.kesp.processor.extensions.isEmpty
import com.github.darkxanter.kesp.processor.extensions.isMatched
import com.github.darkxanter.kesp.processor.extensions.panic
import com.github.darkxanter.kesp.processor.generator.model.ColumnDefinition
import com.github.darkxanter.kesp.processor.generator.model.ProjectionDefinition
import com.github.darkxanter.kesp.processor.generator.model.TableDefinition
import com.github.darkxanter.kesp.processor.helpers.createFile
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo

internal class ExposedTableGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val configuration: Configuration,
) : KSVisitorVoid() {
    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        logger.info("visit", classDeclaration)

        val exposedTable = classDeclaration.getAnnotationsByType(ExposedTable::class).first()

        val columns = getTableColumns(classDeclaration)
        val projections = getProjectionDefinitions(classDeclaration, columns)
        val tableDefinition = TableDefinition(classDeclaration, columns, projections, exposedTable)

        logger.info("tableDefinition $tableDefinition")
        logger.info("allColumns ${tableDefinition.allColumns}")
        logger.info("primaryKey ${tableDefinition.primaryKey}")

        if (exposedTable.models) {
            writeFile(tableDefinition, "${tableDefinition.tableName}Models") {
                generateModels(tableDefinition, configuration, logger)
            }
        }
        if (exposedTable.tableFunctions) {
            writeFile(tableDefinition, "${tableDefinition.tableName}Functions") {
                generateTableFunctions(tableDefinition, logger)
            }
        }
        if (exposedTable.crudRepository) {
            writeFile(tableDefinition, "${tableDefinition.tableName}Repository") {
                generateCrudRepository(tableDefinition, logger)
            }
        }
        if (exposedTable.generateDao) {
//            val foreignKeys = getForeignKeys(classDeclaration)

            writeFile(tableDefinition, "${tableDefinition.tableName}Dao") {
                generateDao(tableDefinition, /*foreignKeys,*/ logger)
            }
        }
    }

    private inline fun writeFile(
        tableDefinition: TableDefinition,
        fileName: String,
        crossinline builder: FileSpec.Builder.() -> Unit,
    ) {
        createFile(tableDefinition.packageName, fileName, builder).writeTo(
            codeGenerator,
            aggregating = false,
            originatingKSFiles = listOfNotNull(
                tableDefinition.declaration.containingFile,
            )
        )
    }


    @OptIn(KspExperimental::class)
    private fun getTableColumns(classDeclaration: KSClassDeclaration): List<ColumnDefinition> {
        val isDefaultExposedTable = classDeclaration.getAllSuperTypes().any {
            it.isMatched("org.jetbrains.exposed.dao.id.IdTable")
        }

        return classDeclaration.getAllProperties().filter {
            it.simpleName.asString() != "autoIncColumn"
        }.mapNotNull { declaration ->
            val columnType = declaration.type.resolve()
            if (!columnType.isMatched("org.jetbrains.exposed.sql.Column")) {
                return@mapNotNull null
            }
            val columnName = declaration.simpleName.asString()

            val isGeneratedColumn = isDefaultExposedTable && columnName == "id"
                || declaration.getAnnotationsByType(GeneratedValue::class).isEmpty().not()

            val isPrimaryKey = isDefaultExposedTable && columnName == "id"
                || declaration.getAnnotationsByType(Id::class).isEmpty().not()

//            val foreignKey = declaration.filterAnnotations(ForeignKey::class).firstOrNull()?.let { annotation ->
//                ForeignKeyDefinition(
//                    table = annotation.getValue(ForeignKey::table),
//                    dao = annotation.getValue(ForeignKey::dao),
//                    targetColumn = annotation.getValue(ForeignKey::column),
//                    sourceColumn = classDeclaration.simpleName.asString(),
//                )
//            }
//            logger.info("foreignKey $foreignKey")

            ColumnDefinition(
                name = columnName,
                type = columnType.getFirstArgumentType(),
                generated = isGeneratedColumn,
                primaryKey = isPrimaryKey,
                docString = declaration.docString,
            )
        }.sortedBy {
            if (it.name == "id") 0 else 1
        }.toList()
    }

    data class ProjectionConfig(
        val type: KSType,
        val readFunction: Boolean,
        val updateFunction: Boolean,
    )

    private fun getProjections(classDeclaration: KSClassDeclaration): Sequence<ProjectionConfig> {
        val projections = classDeclaration.filterAnnotations(Projection::class).map { annotation ->
            logger.info("annotation ${annotation.arguments}")

            val type = annotation.arguments.find {
                it.name?.asString() == Projection::dataClass.name
            }?.value as? KSType ?: logger.panic("couldn't cast dataClass to KSType", annotation)

            val readFunction = annotation.arguments.find {
                it.name?.asString() == Projection::readFunction.name
            }?.value as? Boolean ?: true

            val updateFunction = annotation.arguments.find {
                it.name?.asString() == Projection::updateFunction.name
            }?.value as? Boolean ?: false

            ProjectionConfig(
                type = type,
                readFunction = readFunction,
                updateFunction = updateFunction,
            )
        }
        return projections.distinctBy { it.type }
    }

    private fun getProjectionDefinitions(
        classDeclaration: KSClassDeclaration,
        columns: List<ColumnDefinition>,
    ): List<ProjectionDefinition> {
        return getProjections(classDeclaration).map { projection ->

            logger.info("projection $projection")

            val className = projection.type.declaration.qualifiedName?.asString()

            val projectionDeclaration = projection.type.declaration as? KSClassDeclaration
                ?: logger.panic(
                    "$className is not class",
                    projection.type.declaration
                )

            val isInterface = projectionDeclaration.classKind == ClassKind.INTERFACE

            val projectionColumns = if (isInterface) {
                projectionDeclaration.getAllProperties().mapNotNull { property ->
                    columns.find { it.name == property.simpleName.asString() }
                }.toList()
            } else {
                val primaryConstructor = projectionDeclaration.primaryConstructor
                    ?: logger.panic(
                        "$className doesn't have a primary constructor",
                        projectionDeclaration
                    )
                primaryConstructor.parameters.mapNotNull { parameter ->
                    val column = columns.find { it.name == parameter.name?.asString() }
                    if (column == null && !parameter.hasDefault && projection.readFunction) {
                        logger.panic(
                            "$className parameter $parameter doesn't exists " +
                                "in ${classDeclaration.simpleName.asString()} or has default value",
                            projectionDeclaration
                        )
                    }
                    column
                }
            }

            ProjectionDefinition(
                type = projection.type,
                columns = projectionColumns,
                readFunction = if (isInterface) false else projection.readFunction,
                updateFunction = if (isInterface) true else projection.updateFunction,
            )
        }.toList()
    }


//    private fun getForeignKeys(classDeclaration: KSClassDeclaration): List<ForeignKeyDefinition> {
//        return classDeclaration.filterAnnotations(ForeignKey::class).map { annotation ->
//            ForeignKeyDefinition(
//                table = annotation.getValue(ForeignKey::table),
//                dao = annotation.getValue(ForeignKey::dao),
//                targetColumn = annotation.getValue(ForeignKey::column),
//                sourceColumn = classDeclaration.simpleName.asString(),
//            )
//        }.toList()
//    }
}
