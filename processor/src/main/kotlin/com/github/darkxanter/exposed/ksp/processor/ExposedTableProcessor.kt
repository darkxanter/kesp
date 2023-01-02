package com.github.darkxanter.exposed.ksp.processor

import com.github.darkxanter.exposed.ksp.annotation.ExposedTable
import com.github.darkxanter.exposed.ksp.processor.extensions.getSymbolsWithAnnotation
import com.github.darkxanter.exposed.ksp.processor.extensions.isEmpty
import com.github.darkxanter.exposed.ksp.processor.generator.ExposedTableGenerator
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

public class ExposedTableProcessor(
    codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val exposedTableGenerator = ExposedTableGenerator(codeGenerator, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Start exposed-ksp processing round")
        val resolvedSymbols = resolver.getSymbolsWithAnnotation<ExposedTable>()

        val invalidSymbols = if (!resolvedSymbols.isEmpty()) {
            val (validTrackers, invalidTrackers) = resolvedSymbols.partition { it.validate() }
            validTrackers.filter { symbol ->
                symbol.isSymbolValid()
            }.forEach { classDeclaration ->
                classDeclaration.accept(exposedTableGenerator, Unit)
            }
            invalidTrackers
        } else {
            emptyList()
        }
        logger.info("Finish exposed-ksp processing round")
        return invalidSymbols
    }

    private fun KSAnnotated.isSymbolValid(): Boolean = when {
        this is KSClassDeclaration && classKind == ClassKind.OBJECT -> true
        else -> {
            logger.error("@ExposedTable can be applied only to object")
            false
        }
    }
}
