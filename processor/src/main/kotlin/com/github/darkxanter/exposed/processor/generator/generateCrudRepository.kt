package com.github.darkxanter.exposed.processor.generator

import com.github.darkxanter.exposed.processor.helpers.addClass
import com.github.darkxanter.exposed.processor.helpers.addCodeBlock
import com.github.darkxanter.exposed.processor.helpers.addFunction
import com.github.darkxanter.exposed.processor.helpers.addParameter
import com.github.darkxanter.exposed.processor.helpers.addReturn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

internal fun FileSpec.Builder.generateCrudRepository(tableDefinition: TableDefinition) {
    val tableName = tableDefinition.tableName
    val selectTypeName = LambdaTypeName.get(
        receiver = ClassName("org.jetbrains.exposed.sql", "SqlExpressionBuilder"),
        returnType = ClassName("org.jetbrains.exposed.sql", "Op").parameterizedBy(Boolean::class.asTypeName())
    )

    addImport("org.jetbrains.exposed.sql.transactions", "transaction")
    addImport("org.jetbrains.exposed.sql", "selectAll", "select", "deleteWhere")
    addImport("org.jetbrains.exposed.sql.SqlExpressionBuilder", "eq")

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

        // TODO handle id column with another name
        tableDefinition.idColumnClassName?.let { className ->
            addFunction("findById") {
                returns(tableDefinition.fullDtoClassName.copy(nullable = true))
                addParameter("id", className)
                addStatement("")
                addReturn()
                beginControlFlow("findOne")
                addStatement("$tableName.id.eq(id)")
                endControlFlow()
            }
        }

        addFunction("create") {
            addParameter("dto", tableDefinition.createInterfaceClassName)
            transactionBlock {
                addStatement("$tableName.${tableDefinition.insertDtoFunName}(dto)")
            }
        }

        // TODO handle id column with another name
        tableDefinition.idColumnClassName?.let { idClassName ->
            addFunction("update") {
                addParameter("id", idClassName)
                addParameter("dto", tableDefinition.createInterfaceClassName)
                transactionBlock {
                    addStatement("$tableName.${tableDefinition.updateDtoFunName}(id, dto)")
                }
            }
        }

        // TODO handle id column with another name
        tableDefinition.idColumnClassName?.let { className ->
            addFunction("deleteById") {
                returns(Int::class)
                addParameter("id", className)
                addStatement("")
                addReturn()
                beginControlFlow("delete")
                addStatement("$tableName.id.eq(id)")
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
