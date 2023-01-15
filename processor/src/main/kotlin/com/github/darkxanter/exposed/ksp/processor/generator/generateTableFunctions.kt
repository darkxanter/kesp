package com.github.darkxanter.exposed.ksp.processor.generator

import com.github.darkxanter.exposed.ksp.processor.helpers.CallableParam
import com.github.darkxanter.exposed.ksp.processor.helpers.addCall
import com.github.darkxanter.exposed.ksp.processor.helpers.addCodeBlock
import com.github.darkxanter.exposed.ksp.processor.helpers.addColumnsAsParameters
import com.github.darkxanter.exposed.ksp.processor.helpers.addFunction
import com.github.darkxanter.exposed.ksp.processor.helpers.addReturn
import com.github.darkxanter.exposed.ksp.processor.helpers.endControlFlow
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.asClassName

private const val MAPPING_FROM_FUN_NAME = "fromDto"

internal fun FileSpec.Builder.generateTableFunctions(tableDefinition: TableDefinition) {
    val interfaceClassName = tableDefinition.createInterfaceClassName

    generateTableFunctions(interfaceClassName, tableDefinition)
    generateMappingFunctions(interfaceClassName, tableDefinition)
}

private fun FileSpec.Builder.generateTableFunctions(
    interfaceClassName: ClassName,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName
    val tableClassName = tableDefinition.tableClassName
    val primaryKey = tableDefinition.primaryKey

    addImport(
        "org.jetbrains.exposed.sql",
        if (primaryKey.size == 1) "insertAndGetId" else "insert",
        "update"
    )
    if (primaryKey.size > 1) {
        addImport(
            "org.jetbrains.exposed.sql",
            "and"
        )
    }

    val insertFun = if (primaryKey.size == 1) "insertAndGetId" else "insert"

    val updateWhere = primaryKey.mapIndexed { index, column ->
        val statement = "$tableName.${column.name}.eq(${column.name})"
        if (index > 0)
            ".and($statement)"
        else
            statement
    }.joinToString("")

    val updateFun = "$tableName.update({$updateWhere})"

    addFunction(tableDefinition.insertDtoFunName) {
        receiver(tableDefinition.tableClassName)
        addParameter("dto", interfaceClassName)

        primaryKey.singleOrNull()?.let {
            returns(it.className)
            addReturn()
        }

        addCodeBlock {
            beginControlFlow("$tableName.$insertFun")
            addStatement("it.$MAPPING_FROM_FUN_NAME(dto)")

            if (primaryKey.size == 1 && primaryKey.first().isEntityId) {
                endControlFlow(".value")
            } else {
                endControlFlow()
            }
        }
    }

    if (primaryKey.isNotEmpty()) {
        addFunction(tableDefinition.updateDtoFunName) {
            receiver(tableClassName)
            returns(Int::class)

            addColumnsAsParameters(primaryKey)
            addParameter("dto", interfaceClassName)

            addReturn()
            addCodeBlock {
                beginControlFlow(updateFun)
                addStatement("it.$MAPPING_FROM_FUN_NAME(dto)")
                endControlFlow()
            }
        }
    }

    addFunction(tableDefinition.insertDtoFunName) {
        receiver(tableClassName)
        addColumnsAsParameters(tableDefinition.explicitColumns)

        primaryKey.singleOrNull()?.let {
            returns(it.className)
            addReturn()
        }

        addCodeBlock {
            beginControlFlow("$tableName.$insertFun")
            addCall("it.$MAPPING_FROM_FUN_NAME", tableDefinition.explicitColumns) { column ->
                CallableParam(column.name, column.name)
            }
            if (primaryKey.size == 1 && primaryKey.first().isEntityId) {
                endControlFlow(".value")
            } else {
                endControlFlow()
            }
        }
    }

    if (primaryKey.isNotEmpty() && tableDefinition.commonColumns.isNotEmpty()) {
        addFunction(tableDefinition.updateDtoFunName) {
            receiver(tableClassName)
            returns(Int::class)

            val columns = setOf(
                primaryKey,
                tableDefinition.explicitColumns,
            ).flatten()

            addColumnsAsParameters(columns)

            addReturn()
            addCodeBlock {
                beginControlFlow(updateFun)
                addCall("it.$MAPPING_FROM_FUN_NAME", tableDefinition.commonColumns) { column ->
                    CallableParam(column.name, column.name)
                }
                endControlFlow()
            }
        }
    }
}

private fun FileSpec.Builder.generateMappingFunctions(
    interfaceClassName: ClassName,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName
    val resultRowClassName = ClassName("org.jetbrains.exposed.sql", "ResultRow")

    addImport("org.jetbrains.exposed.sql", "ResultRow")

    // read

    addFunction(tableDefinition.toDtoFunName) {
        receiver(resultRowClassName)
        returns(tableDefinition.fullDtoClassName)
        addCodeBlock {
            addReturn()
            addCall(tableDefinition.fullDtoClassName.simpleName, tableDefinition.allColumns) { column ->
                val name = column.name
                val unwrap = if (column.isEntityId) ".value" else ""
                CallableParam(name, "this[$tableName.$name]$unwrap")
            }
        }
    }

    addFunction(tableDefinition.toDtoListFunName) {
        receiver(Iterable::class.asClassName().parameterizedBy(resultRowClassName))
        returns(List::class.asClassName().parameterizedBy(tableDefinition.fullDtoClassName))
        addCodeBlock {
            addReturn()
            beginControlFlow("map")
            addStatement("it.${tableDefinition.toDtoFunName}()")
            endControlFlow()
        }
    }

    // write

    val updateBuilderClassName = ClassName(
        "org.jetbrains.exposed.sql.statements", "UpdateBuilder"
    ).parameterizedBy(STAR)

    addFunction(MAPPING_FROM_FUN_NAME) {
        receiver(updateBuilderClassName)
        addParameter("dto", interfaceClassName)

        tableDefinition.explicitColumns.forEach { column ->
            val name = column.name
            addStatement("this[$tableName.$name] = dto.$name")
        }
    }

    addFunction(MAPPING_FROM_FUN_NAME) {
        receiver(updateBuilderClassName)
        addColumnsAsParameters(tableDefinition.explicitColumns)
        tableDefinition.explicitColumns.forEach { column ->
            val name = column.name
            addStatement("this[$tableName.$name] = $name")
        }
    }
}
