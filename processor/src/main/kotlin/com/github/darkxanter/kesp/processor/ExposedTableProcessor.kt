package com.github.darkxanter.kesp.processor

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.processor.extensions.getSymbolsWithAnnotation
import com.github.darkxanter.kesp.processor.extensions.isEmpty
import com.github.darkxanter.kesp.processor.generator.ExposedTableGenerator
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

public class ExposedTableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Start kesp processing round")
        val configuration = Configuration(
            kotlinxSerialization = options["kesp.kotlinxSerialization"]?.toBoolean() ?: false
        )
        logger.info("$configuration")
        val exposedTableGenerator = ExposedTableGenerator(codeGenerator, logger, configuration)

        val resolvedSymbols = resolver.getSymbolsWithAnnotation<ExposedTable>()

        val invalidSymbols = if (!resolvedSymbols.isEmpty()) {
            val (validSymbols, invalidSymbols) = resolvedSymbols.partition { it.validate() }
            validSymbols.filter { symbol ->
                symbol.isSymbolValid()
            }.forEach { classDeclaration ->
                classDeclaration.accept(exposedTableGenerator, Unit)
            }
            invalidSymbols
        } else {
            emptyList()
        }
        logger.info("Finish kesp processing round")
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
