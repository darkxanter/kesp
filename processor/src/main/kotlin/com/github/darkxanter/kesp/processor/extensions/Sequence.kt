package com.github.darkxanter.kesp.processor.extensions

internal fun <T : Any> Sequence<T>.isEmpty(): Boolean = !iterator().hasNext()
