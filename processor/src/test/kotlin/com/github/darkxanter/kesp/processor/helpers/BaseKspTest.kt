package com.github.darkxanter.kesp.processor.helpers

import com.github.darkxanter.kesp.processor.ExposedTableProcessorProvider
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.assertEquals

open class BaseKspTest {
    protected fun compileFilesWithGeneratedSources(
        sourceFiles: List<SourceFile>,
        expectedFiles: List<KotlinFile>,
        kspProcessorProvider: SymbolProcessorProvider = ExposedTableProcessorProvider(),
    ): KotlinCompilation.Result {
        println("Generate KSP sources.")
        val generateKspSourcesCompilation = kotlinCompilation(sourceFiles) {
            symbolProcessorProviders = listOf(kspProcessorProvider)
        }
        val generateSourcesResult = generateKspSourcesCompilation.compile()
        assertEquals(
            KotlinCompilation.ExitCode.OK,
            generateSourcesResult.exitCode,
            message = "Generate KSP sources failed.",
        )
        println("Compile KSP sources.")
        val kspGeneratedSourceFiles = generateSourcesResult.kspGeneratedSources().kspGeneratedSourceFiles()

        val compileSourcesResult = kotlinCompilation(
            kspGeneratedSourceFiles + generateKspSourcesCompilation.sources
        ).compile()
        assertEquals(
            KotlinCompilation.ExitCode.OK,
            compileSourcesResult.exitCode,
            message = "Compile KSP sources failed.",
        )

        if (expectedFiles.isNotEmpty()) {
            println("Compare generated files.")
            compareFiles(
                sourceFiles = sourceFiles,
                expectedFiles = expectedFiles,
                actualFiles = generateSourcesResult.kspGeneratedSources().ktFiles(),
            )
        }

        return compileSourcesResult
    }

    private fun kotlinCompilation(
        files: List<SourceFile>,
        configure: KotlinCompilation.() -> Unit = {},
    ) = KotlinCompilation().apply {
        sources = files.toList()
        inheritClassPath = true
        messageOutputStream = System.out
        configure()
    }

    private fun compareFiles(
        sourceFiles: List<SourceFile>,
        expectedFiles: List<KotlinFile>,
        actualFiles: List<KotlinFile>,
    ) {
//        val expectedResult = kotlinCompilation(
//            files = sourceFiles + expectedFiles.toSourceFiles()
//        ).compile()
//        assertEquals(
//            KotlinCompilation.ExitCode.OK,
//            expectedResult.exitCode,
//            message = "Compile expected sources failed.",
//        )

        assertEquals(expectedFiles.size, actualFiles.size, message = "Unexpected count of generated files.")

        val expectedFilesSorted = expectedFiles.sortedBy { it.name }
        val actualFilesSorted = actualFiles.sortedBy { it.name }

        expectedFilesSorted.zip(actualFilesSorted).forEach { (expected, actual) ->
            assertEquals(expected.name, actual.name, "File names don't match")
            assertEquals(expected.code.trim(), actual.code.trim(), "File ${expected.name} don't match")
//            assertThat(actual.code.trim()).isEqualTo(expected.code.trim())
        }
    }
}
