package com.github.darkxanter.exposed.processor.extensions

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

internal inline fun <reified T : Any> Resolver.getSymbolsWithAnnotation(): Sequence<KSAnnotated> =
    getSymbolsWithAnnotation(T::class.getQualifiedName())
