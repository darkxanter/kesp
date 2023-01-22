package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.processor.helpers.addClass
import com.github.darkxanter.kesp.processor.helpers.addCodeBlock
import com.github.darkxanter.kesp.processor.helpers.addFunction
import com.github.darkxanter.kesp.processor.helpers.addParameter
import com.github.darkxanter.kesp.processor.helpers.addReturn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock

internal fun FileSpec.Builder.generateCrudRepository(tableDefinition: TableDefinition) {
    val tableName = tableDefinition.tableName
    val selectTypeName = LambdaTypeName.get(
        receiver = ClassName("org.jetbrains.exposed.sql", "SqlExpressionBuilder"),
        returnType = ClassName("org.jetbrains.exposed.sql", "Op").parameterizedBy(Boolean::class.asTypeName())
    )

    addImport("org.jetbrains.exposed.sql.transactions", "transaction")
    addImport("org.jetbrains.exposed.sql", "selectAll", "select", "deleteWhere", "and")
    addImport("org.jetbrains.exposed.sql.SqlExpressionBuilder", "eq")

    val whereBlock = buildCodeBlock {
        tableDefinition.primaryKey.first().let {
            addStatement("$tableName.${it.name}.eq(${it.name})")
        }
        tableDefinition.primaryKey.drop(1).forEach {
            addStatement(".and($tableName.${it.name}.eq(${it.name}))")
        }
    }

    addClass(tableDefinition.repositoryClassName) {
        addModifiers(KModifier.OPEN)

        addFunction("find") {
            returns(List::class.asClassName().parameterizedBy(tableDefinition.fullDtoClassName))

            addParameter("where", selectTypeName.copy(nullable = true)) {
                defaultValue("%L", null)
            }

            addStatement("")
            addReturn()
            transactionBlock {
                beginControlFlow("if (where != null)")
                addStatement("$tableName.select(where).${tableDefinition.toDtoListFunName}()")
                nextControlFlow("else")
                addStatement("$tableName.selectAll().${tableDefinition.toDtoListFunName}()")
                endControlFlow()
            }
        }

        addFunction("findOne") {
            returns(tableDefinition.fullDtoClassName.copy(nullable = true))
            addParameter("where", selectTypeName)

            addStatement("")
            addReturn()
            addStatement("find(where).singleOrNull()")
        }

        if (tableDefinition.primaryKey.isNotEmpty()) {
            addFunction("findById") {
                returns(tableDefinition.fullDtoClassName.copy(nullable = true))
                tableDefinition.primaryKey.forEach {
                    addParameter(it.name, it.className)
                }
                addStatement("")
                addReturn()

                beginControlFlow("findOne")
                addCode(whereBlock)
                endControlFlow()
            }
        }

        addFunction("create") {
            addParameter("dto", tableDefinition.createInterfaceClassName)
            tableDefinition.primaryKey.singleOrNull()?.let {
                returns(it.className)
                addReturn()
            }
            transactionBlock {
                addStatement("$tableName.${tableDefinition.insertDtoFunName}(dto)")
            }
        }


        if (tableDefinition.hasUpdateFun) {
            addFunction("update") {
                returns(Int::class)
                tableDefinition.primaryKey.forEach {
                    addParameter(it.name, it.className)
                }
                addParameter("dto", tableDefinition.createInterfaceClassName)

                addReturn()
                transactionBlock {
                    val primaryKey = tableDefinition.primaryKey.joinToString(", ") { it.name }
                    addStatement("$tableName.${tableDefinition.updateDtoFunName}($primaryKey, dto)")
                }
            }
        }

        if (tableDefinition.primaryKey.isNotEmpty()) {
            addFunction("deleteById") {
                returns(Int::class)
                tableDefinition.primaryKey.forEach {
                    addParameter(it.name, it.className)
                }
                addStatement("")
                addReturn()
                beginControlFlow("delete")
                addCode(whereBlock)
                endControlFlow()
            }
        }

        addFunction("delete") {
            returns(Int::class)
            val whereTypeName = LambdaTypeName.get(
                receiver = tableDefinition.tableClassName,
                ClassName("org.jetbrains.exposed.sql", "ISqlExpressionBuilder"),
                returnType = ClassName("org.jetbrains.exposed.sql", "Op")
                    .parameterizedBy(Boolean::class.asTypeName())
            )
            addParameter("where", whereTypeName)
            addStatement("")
            addReturn()
            transactionBlock {
                beginControlFlow("$tableName.deleteWhere")
                addStatement("where(it)")
                endControlFlow()
            }
        }
    }
}


private fun FunSpec.Builder.transactionBlock(transaction: CodeBlock.Builder.() -> Unit) {
    addCodeBlock {
        beginControlFlow("transaction")
        transaction()
        endControlFlow()
    }
}
