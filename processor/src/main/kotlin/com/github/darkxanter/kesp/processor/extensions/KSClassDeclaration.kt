package com.github.darkxanter.kesp.processor.extensions

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

internal fun KSClassDeclaration.toClassName(name: String) = ClassName(packageName.asString(), name)

