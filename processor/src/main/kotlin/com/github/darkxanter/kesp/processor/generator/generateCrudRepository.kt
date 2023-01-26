package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.processor.generator.model.ColumnDefinition
import com.github.darkxanter.kesp.processor.generator.model.TableDefinition
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
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock

private val selectTypeName = LambdaTypeName.get(
    receiver = ClassName("org.jetbrains.exposed.sql", "SqlExpressionBuilder"),
    returnType = ClassName("org.jetbrains.exposed.sql", "Op").parameterizedBy(Boolean::class.asTypeName())
)

private val queryTypeName = LambdaTypeName.get(
    receiver = ClassName("org.jetbrains.exposed.sql", "Query"),
    returnType = UNIT,
)

internal fun FileSpec.Builder.generateCrudRepository(tableDefinition: TableDefinition) {
    val tableName = tableDefinition.tableName


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

        addFindFunction(
            name = "find",
            tableName = tableName,
            listClassName = tableDefinition.fullDtoClassName,
            toFunctionName = tableDefinition.toDtoListFunName,
        )

        tableDefinition.projections.forEach { projection ->
            addFindFunction(
                name = "find${projection.className.simpleName}",
                tableName = tableName,
                listClassName = projection.className,
                toFunctionName = projection.toFunctionListName,
                sliceColumns = projection.columns,
            )
        }

        addFunction("findOne") {
            returns(tableDefinition.fullDtoClassName.copy(nullable = true))
            addParameter("where", selectTypeName)

            addStatement("")
            addReturn()
            addStatement("find(where = where).singleOrNull()")
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
            addUpdateFunction(
                name = "update",
                dtoClassName = tableDefinition.createInterfaceClassName,
                tableDefinition = tableDefinition,
            )
            tableDefinition.projections.filter { it.updateFunction }.forEach {
                addUpdateFunction(
                    name = "update${it.className.simpleName}",
                    dtoClassName = it.className,
                    tableDefinition = tableDefinition,
                )
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


private fun TypeSpec.Builder.addFindFunction(
    name: String,
    tableName: String,
    listClassName: ClassName,
    toFunctionName: String,
    sliceColumns: List<ColumnDefinition> = emptyList(),
) {
    val toFun = "${toFunctionName}()"

    val slice = if (sliceColumns.isNotEmpty())
        sliceColumns.joinToString(",", prefix = ".slice(", postfix = ")") {
            "$tableName.${it.name}"
        }
    else ""

    addFunction(name) {
        returns(List::class.asClassName().parameterizedBy(listClassName))

        addParameter("configure", queryTypeName) {
            defaultValue("%L", "{}")
        }

        addParameter("where", selectTypeName.copy(nullable = true)) {
            defaultValue("%L", null)
        }

        addStatement("")
        addReturn()
        transactionBlock {
            beginControlFlow("if (where != null)")
            addStatement("${tableName}${slice}.select(where).apply(configure).$toFun")
            nextControlFlow("else")
            addStatement("${tableName}${slice}.selectAll().apply(configure).$toFun")
            endControlFlow()
        }
    }
}


private fun TypeSpec.Builder.addUpdateFunction(
    name: String,
    dtoClassName: ClassName,
    tableDefinition: TableDefinition,
) {
    val tableName = tableDefinition.tableName

    addFunction(name) {
        returns(Int::class)
        tableDefinition.primaryKey.forEach {
            addParameter(it.name, it.className)
        }
        addParameter("dto", dtoClassName)

        addReturn()
        transactionBlock {
            val primaryKey = tableDefinition.primaryKey.joinToString(", ") { it.name }
            addStatement("$tableName.${tableDefinition.updateDtoFunName}($primaryKey, dto)")
        }
    }
}
