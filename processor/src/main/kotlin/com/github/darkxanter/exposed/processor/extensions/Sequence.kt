package com.github.darkxanter.exposed.processor.extensions

internal fun <T : Any> Sequence<T>.isEmpty(): Boolean = !iterator().hasNext()
