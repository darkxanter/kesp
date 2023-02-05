package com.github.darkxanter.kesp.processor.helpers

import org.intellij.lang.annotations.Language

data class KotlinFile(
    val name: String,
    @Language("kotlin")
    val code: String,
) {
    fun toSourceFile() = ktSourceFile(name, code)
}

fun List<KotlinFile>.toSourceFiles() = map { it.toSourceFile() }
