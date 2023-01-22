package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.annotation.GeneratedValue
import com.github.darkxanter.kesp.annotation.Id
import com.github.darkxanter.kesp.processor.extensions.getFirstArgumentType
import com.github.darkxanter.kesp.processor.extensions.isEmpty
import com.github.darkxanter.kesp.processor.extensions.isMatched
import com.github.darkxanter.kesp.processor.extensions.toClassName
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toClassName

@OptIn(KspExperimental::class)
@Suppress("MemberVisibilityCanBePrivate")
internal data class TableDefinition(
    val declaration: KSClassDeclaration,
) {
    val tableName = declaration.simpleName.asString()
    val tableClassName = declaration.toClassName()
    val packageName = declaration.packageName.asString()

    val allColumns = getTableColumns(declaration)
    val primaryKey = allColumns.filter { it.primaryKey }
    val commonColumns = allColumns.filter { !it.generated && !it.primaryKey }
    val generatedColumns = allColumns.filter { it.generated }
    val explicitColumns = allColumns.filter { !it.generated }

    val hasGeneratedColumns = generatedColumns.isNotEmpty()

    val hasUpdateFun = primaryKey.isNotEmpty() && commonColumns.isNotEmpty()

    val createInterfaceClassName = declaration.toClassName("${tableName}Create")
    val fullInterfaceClassName = declaration.toClassName("${tableName}Full")
    val createDtoClassName = declaration.toClassName("${tableName}CreateDto")
    val fullDtoClassName = declaration.toClassName("${tableName}FullDto")
    val repositoryClassName = declaration.toClassName("${tableName}Repository")

    val insertDtoFunName = "insertDto"
    val updateDtoFunName = "updateDto"
    val toDtoFunName = "to${fullDtoClassName.simpleName}"
    val toDtoListFunName = "to${fullDtoClassName.simpleName}List"

    private fun getTableColumns(classDeclaration: KSClassDeclaration): List<ColumnDefinition> {
        val isDefaultExposedTable = declaration.getAllSuperTypes().any {
            it.isMatched(
                "org.jetbrains.exposed.dao.id.IntIdTable",
                "org.jetbrains.exposed.dao.id.LongIdTable",
                "org.jetbrains.exposed.dao.id.UUIDTable",
            )
        }

        return classDeclaration.getAllProperties().filter {
            it.simpleName.asString() != "autoIncColumn"
        }.mapNotNull {
            val columnType = it.type.resolve()
            if (!columnType.isMatched("org.jetbrains.exposed.sql.Column")) {
                return@mapNotNull null
            }
            val columnName = it.simpleName.asString()

            val isGeneratedColumn = isDefaultExposedTable && columnName == "id"
                || it.getAnnotationsByType(GeneratedValue::class).isEmpty().not()

            val isPrimaryKey = isDefaultExposedTable && columnName == "id"
                || it.getAnnotationsByType(Id::class).isEmpty().not()

            ColumnDefinition(
                name = columnName,
                type = columnType.getFirstArgumentType(),
                generated = isGeneratedColumn,
                primaryKey = isPrimaryKey,
                docString = it.docString,
            )
        }.sortedBy {
            if (it.name == "id") 0 else 1
        }.toList()
    }
}
