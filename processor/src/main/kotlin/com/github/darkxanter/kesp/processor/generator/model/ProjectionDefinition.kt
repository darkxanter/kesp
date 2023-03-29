package com.github.darkxanter.kesp.processor.generator.model

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName

internal data class ProjectionDefinition(
    val type: KSType,
    val columns: List<ColumnDefinition>,
    val readFunction: Boolean,
    val updateFunction: Boolean,
) {
    val className = type.toClassName()
    val toFunctionName = "to${className.simpleName}"
    val toFunctionListName = "to${className.simpleName}List"
}
