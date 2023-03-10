package com.github.darkxanter.kesp.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

public class ExposedTableProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ExposedTableProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options,
        )
    }
}
