package com.github.darkxanter.kesp.processor.generator

import com.github.darkxanter.kesp.processor.extensions.panic
import com.github.darkxanter.kesp.processor.generator.model.ColumnDefinition
import com.github.darkxanter.kesp.processor.generator.model.TableDefinition
import com.github.darkxanter.kesp.processor.generator.model.TableKind
import com.github.darkxanter.kesp.processor.helpers.addClass
import com.github.darkxanter.kesp.processor.helpers.addCodeBlock
import com.github.darkxanter.kesp.processor.helpers.addFunction
import com.github.darkxanter.kesp.processor.helpers.addParameter
import com.github.darkxanter.kesp.processor.helpers.addPrimaryConstructor
import com.github.darkxanter.kesp.processor.helpers.addProperty
import com.github.darkxanter.kesp.processor.helpers.addReturn
import com.github.darkxanter.kesp.processor.helpers.createParameter
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock

private fun selectTypeName(tableTypeName: TypeName) = LambdaTypeName.get(
    receiver = ClassName("org.jetbrains.exposed.sql", "SqlExpressionBuilder"),
    returnType = ClassName("org.jetbrains.exposed.sql", "Op").parameterizedBy(Boolean::class.asTypeName()),
    parameters = listOf(
        createParameter("table", tableTypeName)
    )
)

private fun queryTypeName(tableTypeName: TypeName) = LambdaTypeName.get(
    receiver = ClassName("org.jetbrains.exposed.sql", "Query"),
    returnType = UNIT,
    parameters = listOf(
        createParameter("table", tableTypeName)
    )
)

internal fun FileSpec.Builder.generateCrudRepository(tableDefinition: TableDefinition, logger: KSPLogger) {
    val tableName = when (tableDefinition.tableKind) {
        TableKind.Class -> "table"
        TableKind.Object -> tableDefinition.tableName
    }
    val tableTypeName = tableDefinition.tableClassName

    val toFunctionArgs = when (tableDefinition.tableKind) {
        TableKind.Object -> ""
        TableKind.Class -> "table"
    }

    addImport("org.jetbrains.exposed.sql.transactions", "transaction")
    addImport("org.jetbrains.exposed.sql", "selectAll", "deleteWhere", "and")
    addImport("org.jetbrains.exposed.sql.SqlExpressionBuilder", "eq")

    if (tableDefinition.primaryKey.isEmpty()) {
        logger.panic("Primary key not specified for table ${tableDefinition.tableName}", tableDefinition.declaration)
    }

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

        addPrimaryConstructor {
            val databaseClassName = ClassName("org.jetbrains.exposed.sql", "Database").copy(nullable = true)

            when (tableDefinition.tableKind) {
                TableKind.Object -> {}
                TableKind.Class -> {
                    addParameter("table", tableTypeName)
                    addProperty("table", tableTypeName) {
                        initializer("table")
                        addModifiers(KModifier.PROTECTED)
                    }
                }
            }

            addParameter("db", databaseClassName) {
                defaultValue("%L", null)
            }
            addProperty("db", databaseClassName) {
                initializer("db")
                addModifiers(KModifier.PROTECTED)
            }
        }

        if (tableDefinition.configuration.tableFunctions) {

            if (tableDefinition.configuration.models) {
                addFindFunction(
                    name = "find",
                    tableName = tableName,
                    tableTypeName = tableTypeName,
                    listClassName = tableDefinition.fullDtoClassName,
                    toFunctionName = tableDefinition.toDtoListFunName,
                    toFunctionArgs = toFunctionArgs,
                )
            }

            tableDefinition.projections.filter { it.readFunction }.forEach { projection ->
                addFindFunction(
                    name = "find${projection.className.simpleName}",
                    tableName = tableName,
                    tableTypeName = tableTypeName,
                    listClassName = projection.className,
                    toFunctionName = projection.toFunctionListName,
                    toFunctionArgs = toFunctionArgs,
                    selectColumns = projection.columns,
                )
            }

            if (tableDefinition.configuration.models) {
                addFunction("findOne") {
                    returns(tableDefinition.fullDtoClassName.copy(nullable = true))
                    addParameter("where", selectTypeName(tableTypeName))

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

                addFunction("createMultiple") {
                    addParameter("dtos", Iterable::class.asClassName().parameterizedBy(tableDefinition.createInterfaceClassName))
                    tableDefinition.primaryKey.singleOrNull()?.let {
                        returns(List::class.asClassName().parameterizedBy(it.className))
                        addReturn()
                    }
                    transactionBlock {
                        addStatement("$tableName.${tableDefinition.batchInsertDtoFunName}(dtos)")
                    }
                }
            }

            if (tableDefinition.hasUpdateFun) {
                if (tableDefinition.configuration.models) {
                    addUpdateFunction(
                        name = "update",
                        tableName = tableName,
                        dtoClassName = tableDefinition.createInterfaceClassName,
                        tableDefinition = tableDefinition,
                    )
                }
                tableDefinition.projections.filter { it.updateFunction }.forEach {
                    addUpdateFunction(
                        tableName = tableName,
                        name = "update${it.className.simpleName}",
                        dtoClassName = it.className,
                        tableDefinition = tableDefinition,
                    )
                }
            }
        }

        if (tableDefinition.primaryKey.isNotEmpty()) {
            addFunction("deleteById") {
                returns(Int::class)
                tableDefinition.primaryKey.forEach {
                    addParameter(it.name, it.className)
                }
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
        beginControlFlow("transaction(db = db)")
        transaction()
        endControlFlow()
    }
}


private fun TypeSpec.Builder.addFindFunction(
    name: String,
    tableName: String,
    tableTypeName: TypeName,
    listClassName: ClassName,
    toFunctionName: String,
    toFunctionArgs: String,
    selectColumns: List<ColumnDefinition> = emptyList(),
) {
    val toFun = "${toFunctionName}($toFunctionArgs)"

    val select = if (selectColumns.isNotEmpty())
        selectColumns.joinToString(",", prefix = "select(", postfix = ")") {
            "$tableName.${it.name}"
        }
    else "selectAll()"

    addFunction(name) {
        returns(List::class.asClassName().parameterizedBy(listClassName))

        addParameter("configure", queryTypeName(tableTypeName)) {
            defaultValue("%L", "{}")
        }

        addParameter("where", selectTypeName(tableTypeName).copy(nullable = true)) {
            defaultValue("%L", null)
        }

        addStatement("")
        addReturn()
        transactionBlock {
            beginControlFlow("if (where != null)")
            addStatement("${tableName}.${select}.where{where($tableName)}.apply{configure($tableName)}.$toFun")
            nextControlFlow("else")
            addStatement("${tableName}.${select}.apply{configure($tableName)}.$toFun")
            endControlFlow()
        }
    }
}


private fun TypeSpec.Builder.addUpdateFunction(
    name: String,
    tableName: String,
    dtoClassName: ClassName,
    tableDefinition: TableDefinition,
) {
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
