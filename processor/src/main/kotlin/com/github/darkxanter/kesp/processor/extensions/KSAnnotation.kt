package com.github.darkxanter.kesp.processor.extensions

import com.google.devtools.ksp.symbol.KSAnnotation
import kotlin.reflect.KProperty

internal inline fun <reified T> KSAnnotation.getValue(property: KProperty<*>): T {
    val propertyName = property.name
    return arguments.find {
        it.name?.asString() == propertyName
    }?.value as? T ?: error("couldn't cast $propertyName to ${T::class}")
}
