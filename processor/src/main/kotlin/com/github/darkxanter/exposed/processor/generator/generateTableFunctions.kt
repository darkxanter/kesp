package com.github.darkxanter.exposed.processor.generator

import com.github.darkxanter.exposed.processor.helpers.CallableParam
import com.github.darkxanter.exposed.processor.helpers.addCall
import com.github.darkxanter.exposed.processor.helpers.addCodeBlock
import com.github.darkxanter.exposed.processor.helpers.addColumnsAsParameters
import com.github.darkxanter.exposed.processor.helpers.addFunction
import com.github.darkxanter.exposed.processor.helpers.addReturn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

private const val MAPPING_FROM_FUN_NAME = "fromDto"

internal fun FileSpec.Builder.generateTableFunctions(tableDefinition: TableDefinition) {
    val interfaceClassName = if (tableDefinition.hasGeneratedColumns)
        tableDefinition.createInterfaceClassName
    else
        tableDefinition.fullInterfaceClassName

    addImport("org.jetbrains.exposed.sql", "insert", "update", "ResultRow")

    generateTableFunctions(interfaceClassName, tableDefinition)
    generateMappingFunctions(interfaceClassName, tableDefinition)
}

private fun FileSpec.Builder.generateTableFunctions(
    interfaceClassName: ClassName,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName
    val tableClassName = tableDefinition.tableClassName
    val idColumn = tableDefinition.idColumn

    fun getUpdateFun(param: String) = idColumn?.let {
        "$tableName.update({ $tableName.id.eq($param) })"
    } ?: "$tableName.update"

    addFunction(tableDefinition.insertDtoFunName) {
        receiver(tableDefinition.tableClassName)
        addParameter("dto", interfaceClassName)
        addCodeBlock {
            beginControlFlow("$tableName.insert")
            addStatement("it.$MAPPING_FROM_FUN_NAME(dto)")
            endControlFlow()
        }
    }

    addFunction(tableDefinition.updateDtoFunName) {
        receiver(tableClassName)
        idColumn?.let {
            addColumnsAsParameters(listOf(idColumn))
        }
        addParameter("dto", interfaceClassName)
        addCodeBlock {
            beginControlFlow(getUpdateFun("id"))
            addStatement("it.$MAPPING_FROM_FUN_NAME(dto)")
            endControlFlow()
        }
    }

    fun CodeBlock.Builder.fillParams() {
        addCall("it.$MAPPING_FROM_FUN_NAME", tableDefinition.commonColumns) { _, name ->
            CallableParam(name, name)
        }
    }

    addFunction(tableDefinition.insertDtoFunName) {
        receiver(tableClassName)
        addColumnsAsParameters(tableDefinition.commonColumns)
        addCodeBlock {
            beginControlFlow("$tableName.insert")
            fillParams()
            endControlFlow()
        }
    }

    addFunction(tableDefinition.updateDtoFunName) {
        receiver(tableClassName)
        idColumn?.let {
            addColumnsAsParameters(listOf(idColumn))
        }
        addColumnsAsParameters(tableDefinition.commonColumns)
        addCodeBlock {
            beginControlFlow(getUpdateFun("id"))
            fillParams()
            endControlFlow()
        }
    }
}

private fun FileSpec.Builder.generateMappingFunctions(
    interfaceClassName: ClassName,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName
    val resultRowClassName = ClassName("org.jetbrains.exposed.sql", "ResultRow")

    // read

    addFunction(tableDefinition.toDtoFunName) {
        receiver(resultRowClassName)
        returns(tableDefinition.fullDtoClassName)
        addCodeBlock {
            addReturn()
            addCall(tableDefinition.fullDtoClassName.simpleName, tableDefinition.allColumns) { _, name ->
                val isEntityId = name == "id"
                val unwrap = if (isEntityId) ".value" else ""
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
    ).parameterizedBy(Any::class.asTypeName())

    addFunction(MAPPING_FROM_FUN_NAME) {
        receiver(updateBuilderClassName)
        addParameter("dto", interfaceClassName)

        tableDefinition.commonColumns.forEach { column ->
            val name = column.simpleName.asString()
            addStatement("this[$tableName.$name] = dto.$name")
        }
    }

    addFunction(MAPPING_FROM_FUN_NAME) {
        receiver(updateBuilderClassName)
        addColumnsAsParameters(tableDefinition.commonColumns)
        tableDefinition.commonColumns.forEach { column ->
            val name = column.simpleName.asString()
            addStatement("this[$tableName.$name] = $name")
        }
    }
}
