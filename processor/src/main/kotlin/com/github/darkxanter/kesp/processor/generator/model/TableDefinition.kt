package com.github.darkxanter.kesp.processor.generator.model

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.processor.extensions.toClassName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toClassName

@Suppress("MemberVisibilityCanBePrivate")
internal data class TableDefinition(
    val declaration: KSClassDeclaration,
    val allColumns: List<ColumnDefinition>,
    val projections: List<ProjectionDefinition>,
    val configuration: ExposedTable,
) {
    val tableName = declaration.simpleName.asString()
    val tableClassName = declaration.toClassName()
    val packageName = declaration.packageName.asString()


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
    val daoBaseClassName = declaration.toClassName("${tableName}DaoBase")
    val daoClassName = declaration.toClassName("${tableName}Dao")

    val batchInsertDtoFunName = "batchInsertDtos"
    val insertDtoFunName = "insertDto"
    val updateDtoFunName = "updateDto"
    val toDtoFunName = "to${fullDtoClassName.simpleName}"
    val toDtoListFunName = "${toDtoFunName}List"
}
