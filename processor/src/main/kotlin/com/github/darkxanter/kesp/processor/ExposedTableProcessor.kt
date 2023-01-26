package com.github.darkxanter.kesp.processor

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.processor.extensions.getSymbolsWithAnnotation
import com.github.darkxanter.kesp.processor.extensions.panic
import com.github.darkxanter.kesp.processor.generator.ExposedTableGenerator
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
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

        val invalidSymbols = processSymbols(resolvedSymbols, exposedTableGenerator) { symbol ->
            if (symbol !is KSClassDeclaration || symbol.classKind != ClassKind.OBJECT) {
                logger.panic("@ExposedTable can be applied only to object")
            }
        }
        logger.info("Finish kesp processing round")
        return invalidSymbols
    }

    private fun processSymbols(
        resolvedSymbols: Sequence<KSAnnotated>,
        visitor: KSVisitorVoid,
        test: (KSAnnotated) -> Unit,
    ): List<KSAnnotated> {
        resolvedSymbols.filter { it.validate() }.forEach { classDeclaration ->
            test(classDeclaration)
            classDeclaration.accept(visitor, Unit)
        }
        return resolvedSymbols.filterNot { it.validate() }.toList()
    }
}
