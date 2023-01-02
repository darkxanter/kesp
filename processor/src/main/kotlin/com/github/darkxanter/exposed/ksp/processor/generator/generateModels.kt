package com.github.darkxanter.exposed.ksp.processor.generator

import com.github.darkxanter.exposed.ksp.processor.helpers.addClass
import com.github.darkxanter.exposed.ksp.processor.helpers.addColumnsAsParameters
import com.github.darkxanter.exposed.ksp.processor.helpers.addColumnsAsProperties
import com.github.darkxanter.exposed.ksp.processor.helpers.addInterface
import com.github.darkxanter.exposed.ksp.processor.helpers.addPrimaryConstructor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier

internal fun FileSpec.Builder.generateModels(tableDefinition: TableDefinition) {
    if (tableDefinition.hasGeneratedColumns) {
        addInterface(tableDefinition.createInterfaceClassName) {
            addColumnsAsProperties(tableDefinition.commonColumns)
        }
        addClass(tableDefinition.createDtoClassName) {
            addModifiers(KModifier.DATA)
            addSuperinterface(tableDefinition.createInterfaceClassName)
            addPrimaryConstructor {
                addColumnsAsParameters(tableDefinition.commonColumns)
            }
            addColumnsAsProperties(tableDefinition.commonColumns, parameter = true, override = true)
        }

    }

    addInterface(tableDefinition.fullInterfaceClassName) {
        if (tableDefinition.hasGeneratedColumns) {
            addSuperinterface(tableDefinition.createInterfaceClassName)
        }
        addColumnsAsProperties(tableDefinition.generatedColumns)
    }
    addClass(tableDefinition.fullDtoClassName) {
        addModifiers(KModifier.DATA)
        addSuperinterface(tableDefinition.fullInterfaceClassName)
        addPrimaryConstructor {
            addColumnsAsParameters(tableDefinition.allColumns)
        }
        addColumnsAsProperties(tableDefinition.allColumns, parameter = true, override = true)
    }
}
