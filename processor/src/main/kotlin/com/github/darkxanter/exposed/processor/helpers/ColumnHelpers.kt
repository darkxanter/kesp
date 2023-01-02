package com.github.darkxanter.exposed.processor.helpers

import com.github.darkxanter.exposed.processor.extensions.getFirstArgumentType
import com.github.darkxanter.exposed.processor.extensions.unwrapEntityId
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
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
        }
    }
}
