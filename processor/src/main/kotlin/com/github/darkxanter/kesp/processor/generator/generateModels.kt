package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.processor.Configuration
import com.github.darkxanter.kesp.processor.extensions.isAnnotated
import com.github.darkxanter.kesp.processor.helpers.addClass
import com.github.darkxanter.kesp.processor.helpers.addColumnsAsParameters
import com.github.darkxanter.kesp.processor.helpers.addColumnsAsProperties
import com.github.darkxanter.kesp.processor.helpers.addInterface
import com.github.darkxanter.kesp.processor.helpers.addPrimaryConstructor
import com.github.darkxanter.kesp.processor.helpers.createAnnotation
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toTypeName

private val serializableAnnotation = ClassName("kotlinx.serialization", "Serializable")

private val primitiveTypes = listOfNotNull(
    String::class.qualifiedName,
    Int::class.qualifiedName,
    Long::class.qualifiedName,
    Boolean::class.qualifiedName,
).toTypedArray()

internal fun FileSpec.Builder.generateModels(
    tableDefinition: TableDefinition,
    configuration: Configuration,
    logger: KSPLogger,
) {
    if (configuration.kotlinxSerialization) {
        val types = tableDefinition.allColumns.asSequence().flatMap { column ->
            val columnType = column.type.makeNotNullable()
            val argTypes = columnType.arguments.map { it.type?.resolve()?.makeNotNullable() }
            argTypes.ifEmpty { listOf(columnType) }
        }.filterNotNull().filterNot {
            primitiveTypes.contains(it.declaration.qualifiedName?.asString())
        }.filterNot { type ->
            type.declaration.isAnnotated(serializableAnnotation)
        }.toSet()

        logger.info("types $types")

        val annotation = createAnnotation(
            ClassName("kotlinx.serialization", "UseContextualSerialization")
        ) {
            types.forEach { type ->
                addMember("%T::class", type.toTypeName())
            }
        }
        addAnnotation(annotation)
    }

    val columns = tableDefinition.explicitColumns

    addInterface(tableDefinition.createInterfaceClassName) {
        addColumnsAsProperties(columns)
    }
    addClass(tableDefinition.createDtoClassName) {
        addModifiers(KModifier.DATA)
        addSuperinterface(tableDefinition.createInterfaceClassName)

        if (configuration.kotlinxSerialization) {
            addAnnotation(serializableAnnotation)
        }

        addPrimaryConstructor {
            addColumnsAsParameters(columns)
        }
        addColumnsAsProperties(columns, parameter = true, override = true)
    }

    addInterface(tableDefinition.fullInterfaceClassName) {
        addSuperinterface(tableDefinition.createInterfaceClassName)
        addColumnsAsProperties(tableDefinition.generatedColumns)
    }
    addClass(tableDefinition.fullDtoClassName) {
        addModifiers(KModifier.DATA)
        addSuperinterface(tableDefinition.fullInterfaceClassName)

        if (configuration.kotlinxSerialization) {
            addAnnotation(serializableAnnotation)
        }

        addPrimaryConstructor {
            addColumnsAsParameters(tableDefinition.allColumns)
        }
        addColumnsAsProperties(tableDefinition.allColumns, parameter = true, override = true)
    }
}
