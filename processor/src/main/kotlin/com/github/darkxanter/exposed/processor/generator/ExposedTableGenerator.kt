package com.github.darkxanter.exposed.processor.generator

import com.github.darkxanter.exposed.annotation.ExposedTable
import com.github.darkxanter.exposed.processor.helpers.createFile
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
) : KSVisitorVoid() {
    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        logger.info("visit", classDeclaration)

        val exposedTable = classDeclaration.getAnnotationsByType(ExposedTable::class).first()

        val tableDefinition = TableDefinition(classDeclaration)
        logger.info("tableDefinition $tableDefinition")

        if (exposedTable.models) {
            writeFile(tableDefinition, "models") {
                generateModels(tableDefinition)
            }
        }
        if (exposedTable.tableFunctions) {
            writeFile(tableDefinition, "functions") {
                generateTableFunctions(tableDefinition)
            }
        }
        if (exposedTable.crudRepository) {
            writeFile(tableDefinition, "repository") {
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
            originatingKSFiles = listOf(tableDefinition.declaration.containingFile!!)
        )
    }
}
