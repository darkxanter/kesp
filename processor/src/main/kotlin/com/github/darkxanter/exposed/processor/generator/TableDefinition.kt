package com.github.darkxanter.exposed.processor.generator

import com.github.darkxanter.exposed.processor.extensions.getFirstArgumentType
import com.github.darkxanter.exposed.processor.extensions.isMatched
import com.github.darkxanter.exposed.processor.extensions.toClassName
import com.github.darkxanter.exposed.processor.extensions.unwrapEntityId
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName

@Suppress("MemberVisibilityCanBePrivate")
internal data class TableDefinition(
    val declaration: KSClassDeclaration,
) {
    val tableName = declaration.simpleName.asString()
    val tableClassName = declaration.toClassName()
    val packageName = declaration.packageName.asString()

    val allColumns = getTableColumns(declaration)
    val generatedColumns: List<KSPropertyDeclaration>
    val commonColumns: List<KSPropertyDeclaration>
    val idColumn: KSPropertyDeclaration?

    val hasGeneratedColumns get() = generatedColumns.isNotEmpty()

    val idColumnClassName: ClassName?
    val createInterfaceClassName = declaration.toClassName("${tableName}Create")
    val fullInterfaceClassName = declaration.toClassName("${tableName}Full")
    val createDtoClassName = declaration.toClassName("${tableName}CreateDto")
    val fullDtoClassName = declaration.toClassName("${tableName}FullDto")
    val repositoryClassName = declaration.toClassName("${tableName}Repository")

    val insertDtoFunName = "insertDto"
    val updateDtoFunName = "updateDto"
    val toDtoFunName = "to${fullDtoClassName.simpleName}"
    val toDtoListFunName = "to${fullDtoClassName.simpleName}List"

    init {
        val idAutoGenerated = declaration.getAllSuperTypes().any {
            it.isMatched(
                "org.jetbrains.exposed.dao.id.IntIdTable",
                "org.jetbrains.exposed.dao.id.LongIdTable",
                "org.jetbrains.exposed.dao.id.UUIDIdTable",
            )
        }
        // TODO better id column handling
        val (generatedColumns, commonColumns) = allColumns.partition {
            idAutoGenerated && it.simpleName.asString() == "id"
        }
        this.generatedColumns = generatedColumns
        this.commonColumns = commonColumns
        idColumn = generatedColumns.find { it.simpleName.asString() == "id" }
        idColumnClassName = idColumn?.type?.resolve()?.getFirstArgumentType()?.unwrapEntityId()?.toClassName()
    }

    private fun getTableColumns(classDeclaration: KSClassDeclaration): List<KSPropertyDeclaration> {
        return classDeclaration.getAllProperties().filter {
            it.type.resolve().isMatched("org.jetbrains.exposed.sql.Column")
        }.filter {
            it.simpleName.asString() != "autoIncColumn"
        }.sortedBy {
            if (it.simpleName.asString() == "id") 0 else 1
        }.toList()
    }
}
