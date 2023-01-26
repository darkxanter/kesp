package com.github.darkxanter.kesp.processor.extensions

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass

internal fun KSAnnotated.isAnnotated(type: ClassName): Boolean {
    return annotations.any {
        it.shortName.asString() == type.simpleName && it.annotationType.resolve()
            .declaration.qualifiedName?.asString() == type.canonicalName
    }
}

internal fun KSAnnotated.filterAnnotations(annotationKClass: KClass<*>): Sequence<KSAnnotation> {
    return this.annotations.filter {
        it.shortName.getShortName() == annotationKClass.simpleName && it.annotationType.resolve().declaration
            .qualifiedName?.asString() == annotationKClass.qualifiedName
    }.map { it }
}
