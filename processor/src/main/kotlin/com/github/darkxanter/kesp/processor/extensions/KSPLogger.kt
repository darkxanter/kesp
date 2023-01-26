package com.github.darkxanter.kesp.processor.extensions

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

internal fun KSPLogger.panic(message: String, symbol: KSNode? = null): Nothing {
    error(message, symbol)
    throw IllegalArgumentException(message)
}
