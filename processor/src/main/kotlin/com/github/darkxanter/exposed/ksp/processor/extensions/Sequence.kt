package com.github.darkxanter.exposed.ksp.processor.extensions

internal fun <T : Any> Sequence<T>.isEmpty(): Boolean = !iterator().hasNext()
