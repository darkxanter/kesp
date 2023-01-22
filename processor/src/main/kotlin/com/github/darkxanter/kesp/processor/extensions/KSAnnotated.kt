package com.github.darkxanter.kesp.processor.extensions

import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ClassName

internal fun KSAnnotated.isAnnotated(type: ClassName): Boolean {
    return annotations.any {
        it.shortName.asString() == type.simpleName && it.annotationType.resolve()
            .declaration.qualifiedName?.asString() == type.canonicalName
    }
}
