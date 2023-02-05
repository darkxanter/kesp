package com.github.darkxanter.kesp.processor.helpers

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import java.io.File

internal fun KotlinCompilation.Result.kspGeneratedSources() =
    outputDirectory.parentFile.resolve("ksp/sources").walk().filter { it.isFile }.toList()

internal fun ktSourceFile(name: String, @Language("kotlin") code: String) =
    SourceFile.kotlin(name = "$name.kt", contents = code)

internal fun List<File>.kspGeneratedSourceFiles() = map { SourceFile.fromPath(it.absoluteFile) }

internal fun List<File>.ktFiles() = map { KotlinFile(it.name, it.readText()) }
