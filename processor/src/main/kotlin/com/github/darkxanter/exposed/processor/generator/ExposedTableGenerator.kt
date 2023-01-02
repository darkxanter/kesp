package com.github.darkxanter.exposed.processor.generator

import com.github.darkxanter.exposed.annotation.ExposedTable
import com.github.darkxanter.exposed.processor.helpers.createFile
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
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
            createFile(tableDefinition.packageName, "models") {
                generateModels(tableDefinition)
            }.writeTo(codeGenerator, aggregating = false)

        }
        if (exposedTable.tableFunctions) {
            createFile(tableDefinition.packageName, "functions") {
                generateTableFunctions(tableDefinition)
            }.writeTo(codeGenerator, aggregating = false)
        }
        if (exposedTable.crudRepository) {
            createFile(tableDefinition.packageName, "repository") {
                generateCrudRepository(tableDefinition)
            }.writeTo(codeGenerator, aggregating = false)
        }
    }
}
