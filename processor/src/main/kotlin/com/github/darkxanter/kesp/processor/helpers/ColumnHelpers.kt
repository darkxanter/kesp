package com.github.darkxanter.kesp.processor.helpers

import com.github.darkxanter.kesp.processor.extensions.unwrapEntityId
import com.github.darkxanter.kesp.processor.generator.ColumnDefinition
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun FunSpec.Builder.addColumnsAsParameters(
    columns: List<ColumnDefinition>,
    nullDefault: Boolean = true,
) {
    columns.forEach { column ->
        val type = column.type.unwrapEntityId()
        val typeName = type.toTypeName()
        addParameter(column.name, typeName) {
            if (nullDefault && typeName.isNullable) {
                defaultValue("%L", null)
            }
        }
    }
}

internal fun TypeSpec.Builder.addColumnsAsProperties(
    columns: List<ColumnDefinition>,
    parameter: Boolean = false,
    override: Boolean = false,
    builder: PropertySpec.Builder.(type: KSType) -> Unit = {},
) {
    columns.distinct().forEach { column ->
        val type = column.type.unwrapEntityId()
        addProperty(column.name, type.toTypeName()) {
            column.docString?.let {
                addKdoc(it.trim())
            }
            if (parameter) {
                initializer(column.name)
            }
            if (override) {
                addModifiers(KModifier.OVERRIDE)
            }
            builder(type)
        }
    }
}

internal data class CallableParam(val target: String, val source: String)

internal inline fun CodeBlock.Builder.addCall(
    callableName: String,
    columns: List<ColumnDefinition>,
    mappingFun: (column: ColumnDefinition) -> CallableParam,
) {
    add("$callableName(\n")
    indent()
    columns.forEach { column ->
        val param = mappingFun(column)
        addStatement("${param.target} = ${param.source},")
    }
    unindent()
    add(")\n")
}
