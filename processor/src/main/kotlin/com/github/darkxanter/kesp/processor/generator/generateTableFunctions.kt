package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.processor.generator.model.ColumnDefinition
import com.github.darkxanter.kesp.processor.generator.model.TableDefinition
import com.github.darkxanter.kesp.processor.helpers.CallableParam
import com.github.darkxanter.kesp.processor.helpers.addCall
import com.github.darkxanter.kesp.processor.helpers.addCodeBlock
import com.github.darkxanter.kesp.processor.helpers.addColumnsAsParameters
import com.github.darkxanter.kesp.processor.helpers.addFunction
import com.github.darkxanter.kesp.processor.helpers.addReturn
import com.github.darkxanter.kesp.processor.helpers.endControlFlow
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.asClassName

private val resultRowClassName = ClassName("org.jetbrains.exposed.sql", "ResultRow")

internal fun FileSpec.Builder.generateTableFunctions(tableDefinition: TableDefinition, logger: KSPLogger) {
    val interfaceClassName = tableDefinition.createInterfaceClassName

    generateTableFunctions(interfaceClassName, tableDefinition)
    generateMappingFunctions(interfaceClassName, tableDefinition)
}

private fun FileSpec.Builder.generateTableFunctions(
    interfaceClassName: ClassName,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName
    val primaryKey = tableDefinition.primaryKey

    val useInsertAndGetId = primaryKey.size == 1 && tableDefinition.isIdTable

    addImport(
        "org.jetbrains.exposed.sql",
        "batchInsert",
        if (useInsertAndGetId) "insertAndGetId" else "insert",
        "update"
    )
    if (primaryKey.size > 1) {
        addImport(
            "org.jetbrains.exposed.sql",
            "and"
        )
    }

    val insertFun = if (useInsertAndGetId) "insertAndGetId" else "insert"

    val updateWhere = primaryKey.mapIndexed { index, column ->
        val statement = "$tableName.${column.name}.eq(${column.name})"
        if (index > 0)
            ".and($statement)"
        else
            statement
    }.joinToString("")

    val updateFun = "$tableName.update({$updateWhere})"

    fun CodeBlock.Builder.endControlFlowWithPrimaryKey() {
        if (primaryKey.size == 1) {
            val key = primaryKey.first()
            val expr = buildString {
                if (!tableDefinition.isIdTable)
                    append("[$tableName.${key.name}]")
                if (key.isEntityId)
                    append(".value")
            }
            endControlFlow(expr)
        } else {
            endControlFlow()
        }
    }

    fun CodeBlock.Builder.endControlFlowWithListOfPrimaryKeys() {
        if (primaryKey.size == 1) {
            val key = primaryKey.first()
            val suffix = if (key.isEntityId) ".value" else ""
            endControlFlow(".map { it[$tableName.${key.name}]${suffix} }")
        } else {
            endControlFlow()
        }
    }

    if (tableDefinition.configuration.models) {
        addFunction(tableDefinition.batchInsertDtoFunName) {
            receiver(tableDefinition.tableClassName)
            addParameter("dtos", Iterable::class.asClassName().parameterizedBy(interfaceClassName))

            primaryKey.singleOrNull()?.let {
                returns(List::class.asClassName().parameterizedBy(it.className))
                addReturn()
            }

            addCodeBlock {
                beginControlFlow("$tableName.batchInsert(dtos)")
                addStatement("this.$MAPPING_FROM_FUN_NAME(it)")
                endControlFlowWithListOfPrimaryKeys()
            }
        }

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
                endControlFlowWithPrimaryKey()
            }
        }
    }

    if (tableDefinition.hasUpdateFun) {
        if (tableDefinition.configuration.models) {
            generateUpdateFunction(
                tableDefinition,
                interfaceClassName,
                updateFun,
            )
        }

        tableDefinition.projections.filter { it.updateFunction }.forEach {
            generateUpdateFunction(
                tableDefinition,
                it.className,
                updateFun,
            )
        }
    }
}

private fun FileSpec.Builder.generateUpdateFunction(
    tableDefinition: TableDefinition,
    dtoClassName: ClassName,
    updateFun: String,
) {
    addFunction(tableDefinition.updateDtoFunName) {
        receiver(tableDefinition.tableClassName)
        returns(Int::class)

        addColumnsAsParameters(tableDefinition.primaryKey)
        addParameter("dto", dtoClassName)

        addReturn()
        addCodeBlock {
            beginControlFlow(updateFun)
            addStatement("it.$MAPPING_FROM_FUN_NAME(dto)")
            endControlFlow()
        }
    }
}

/// ==============================================================================
/// mappings

private fun FileSpec.Builder.generateMappingFunctions(
    interfaceClassName: ClassName,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName

    addImport("org.jetbrains.exposed.sql", "ResultRow", "Alias")

    // read

    if (tableDefinition.configuration.models) {
        // Default mappings
        generateReadMappings(tableDefinition.fullDtoClassName, tableDefinition.allColumns, tableDefinition)
    }

    tableDefinition.projections.filter { it.readFunction }.forEach { projection ->
        generateReadMappings(projection.className, projection.columns, tableDefinition)
    }

    // write

    if (tableDefinition.configuration.models) {
        // Default mappings
        generateWriteMappings(
            interfaceClassName,
            tableDefinition.explicitColumns,
            tableDefinition,
        )
    }

    tableDefinition.projections.filter { it.updateFunction }.forEach { projection ->
        generateWriteMappings(projection.className, projection.columns, tableDefinition)
    }
}

private fun FileSpec.Builder.generateReadMappings(
    dtoClassName: ClassName,
    columns: List<ColumnDefinition>,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName

    val functionName = "to${dtoClassName.simpleName}"
    val functionListName = "${functionName}List"

    val aliasClassName = ClassName("org.jetbrains.exposed.sql", "Alias")
        .parameterizedBy(tableDefinition.tableClassName)

    addFunction(functionName) {
        receiver(resultRowClassName)
        returns(dtoClassName)
        addCodeBlock {
            addReturn()
            addCall(dtoClassName.simpleName, columns) { column ->
                val name = column.name
                CallableParam(name, "this[$tableName.$name]${column.unwrapEntityId}")
            }
        }
    }

    addFunction(functionName) {
        receiver(resultRowClassName)
        returns(dtoClassName)

        addParameter("alias", aliasClassName)

        addCodeBlock {
            addReturn()
            addCall(dtoClassName.simpleName, columns) { column ->
                val name = column.name
                CallableParam(name, "this[alias[$tableName.$name]]${column.unwrapEntityId}")
            }
        }
    }

    addFunction(functionListName) {
        receiver(Iterable::class.asClassName().parameterizedBy(resultRowClassName))
        returns(List::class.asClassName().parameterizedBy(dtoClassName))
        addCodeBlock {
            addReturn()
            beginControlFlow("map")
            addStatement("it.${functionName}()")
            endControlFlow()
        }
    }

    addFunction(functionListName) {
        receiver(Iterable::class.asClassName().parameterizedBy(resultRowClassName))
        returns(List::class.asClassName().parameterizedBy(dtoClassName))

        addParameter("alias", aliasClassName)

        addCodeBlock {
            addReturn()
            beginControlFlow("map")
            addStatement("it.${functionName}(alias)")
            endControlFlow()
        }
    }
}


private fun FileSpec.Builder.generateWriteMappings(
    dtoClassName: ClassName,
    columns: List<ColumnDefinition>,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName

    val explicitColumns = columns.intersect(tableDefinition.explicitColumns.toSet())

    val updateBuilderClassName = ClassName(
        "org.jetbrains.exposed.sql.statements", "UpdateBuilder"
    ).parameterizedBy(STAR)

    addFunction(MAPPING_FROM_FUN_NAME) {
        receiver(updateBuilderClassName)
        addParameter("dto", dtoClassName)

        explicitColumns.forEach { column ->
            val name = column.name
            addStatement("this[$tableName.$name] = dto.$name")
        }
    }
}
