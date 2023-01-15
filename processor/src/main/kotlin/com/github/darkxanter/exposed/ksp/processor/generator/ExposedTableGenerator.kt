package com.github.darkxanter.exposed.ksp.processor.generator

import com.github.darkxanter.exposed.ksp.annotation.ExposedTable
import com.github.darkxanter.exposed.ksp.processor.Configuration
import com.github.darkxanter.exposed.ksp.processor.helpers.createFile
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
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

        val tableDefinition = TableDefinition(classDeclaration)
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
                generateTableFunctions(tableDefinition)
            }
        }
        if (exposedTable.crudRepository) {
            writeFile(tableDefinition, "${tableDefinition.tableName}Repository") {
                generateCrudRepository(tableDefinition)
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
}
