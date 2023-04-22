package com.github.darkxanter.kesp.processor.generator.model

import com.github.darkxanter.kesp.processor.extensions.isMatched
import com.github.darkxanter.kesp.processor.extensions.unwrapEntityId
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

internal data class ColumnDefinition(
    val name: String,
    val type: KSType,
    val generated: Boolean = false,
    val primaryKey: Boolean = false,
//    val hasClientDefault: Boolean = false,
    val docString: String? = null,
) {
    val sourceClassName = type.toTypeName()
    val className = type.unwrapEntityId().toTypeName().copy(nullable = type.isMarkedNullable)
    val isEntityId = type.isMatched("org.jetbrains.exposed.dao.id.EntityID")
    val isNullable = type.isMarkedNullable
}
