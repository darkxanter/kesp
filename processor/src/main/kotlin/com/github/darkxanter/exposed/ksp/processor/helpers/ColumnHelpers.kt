package com.github.darkxanter.exposed.ksp.processor.helpers

import com.github.darkxanter.exposed.ksp.processor.extensions.getFirstArgumentType
import com.github.darkxanter.exposed.ksp.processor.extensions.unwrapEntityId
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun FunSpec.Builder.addColumnsAsParameters(
    columns: List<KSPropertyDeclaration>,
    nullDefault: Boolean = true,
) {
    columns.forEach { column ->
        val type = column.type.resolve().getFirstArgumentType().unwrapEntityId()
        val typeName = type.toTypeName()
        addParameter(column.simpleName.asString(), typeName) {
            if (nullDefault && typeName.isNullable) {
                defaultValue("%L", null)
            }
        }
    }
}

internal fun TypeSpec.Builder.addColumnsAsProperties(
    columns: List<KSPropertyDeclaration>,
    parameter: Boolean = false,
    override: Boolean = false,
    builder: PropertySpec.Builder.(type: KSType) -> Unit = {},
) {
    columns.forEach { column ->
        val type = column.type.resolve().getFirstArgumentType().unwrapEntityId()
        addProperty(column.simpleName.asString(), type.toTypeName()) {
            column.docString?.let {
                addKdoc(it.trim())
            }
            if (parameter) {
                initializer(column.simpleName.asString())
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
    columns: List<KSPropertyDeclaration>,
    mappingFun: (column: KSPropertyDeclaration, name: String) -> CallableParam,
) {
    add("$callableName(\n")
    indent()
    columns.forEach { column ->
        val name = column.simpleName.asString()
        val param = mappingFun(column, name)
        addStatement("${param.target} = ${param.source},")
    }
    unindent()
    add(")\n")
}
