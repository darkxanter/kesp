package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.processor.extensions.isMatched
import com.github.darkxanter.kesp.processor.extensions.unwrapEntityId
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName

internal data class ColumnDefinition(
    val name: String,
    val type: KSType,
    val generated: Boolean = false,
    val primaryKey: Boolean = false,
//    val hasClientDefault: Boolean = false,
    val docString: String? = null,
) {
    val className = type.unwrapEntityId().toClassName()
    val isEntityId = type.isMatched("org.jetbrains.exposed.dao.id.EntityID")
}
