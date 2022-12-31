package com.github.darkxanter.exposed.processor.generator

import com.github.darkxanter.exposed.annotation.ExposedTable
import com.github.darkxanter.exposed.processor.extensions.getFirstArgumentType
import com.github.darkxanter.exposed.processor.extensions.isMatched
import com.github.darkxanter.exposed.processor.extensions.toClassName
import com.github.darkxanter.exposed.processor.helpers.addClass
import com.github.darkxanter.exposed.processor.helpers.addCodeBlock
import com.github.darkxanter.exposed.processor.helpers.addFunction
import com.github.darkxanter.exposed.processor.helpers.addInterface
import com.github.darkxanter.exposed.processor.helpers.addParameter
import com.github.darkxanter.exposed.processor.helpers.addPrimaryConstructor
import com.github.darkxanter.exposed.processor.helpers.addProperty
import com.github.darkxanter.exposed.processor.helpers.createFile
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class ExposedTableGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : KSVisitorVoid() {
    private val mappingFunName = "fromDto"

    private val KSClassDeclaration.tableName get() = simpleName.asString()
    private val KSClassDeclaration.createInterfaceClassName get() = toClassName("${tableName}Create")
    private val KSClassDeclaration.fullInterfaceClassName get() = toClassName("${tableName}Full")
    private val KSClassDeclaration.createDtoClassName get() = toClassName("${tableName}CreateDto")
    private val KSClassDeclaration.fullDtoClassName get() = toClassName("${tableName}FullDto")

    private val updateBuilderClassName = ClassName(
        "org.jetbrains.exposed.sql.statements", "UpdateBuilder"
    ).parameterizedBy(Any::class.asTypeName())


    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        logger.info("visit", classDeclaration)

        val exposedTable = classDeclaration.getAnnotationsByType(ExposedTable::class).first()

        val idAutoGenerated = classDeclaration.getAllSuperTypes().any {
            it.isMatched(
                "org.jetbrains.exposed.dao.id.IntIdTable",
                "org.jetbrains.exposed.dao.id.LongIdTable",
                "org.jetbrains.exposed.dao.id.UUIDIdTable",
            )
        }
        logger.info("idAutoGenerated $idAutoGenerated")

        val packageName = classDeclaration.packageName.asString()
        val columns = getTableColumns(classDeclaration)

        // TODO better id column handling
        val (autoGeneratedColumns, otherColumns) = columns.partition {
            idAutoGenerated && it.simpleName.asString() == "id"
        }

        if (exposedTable.models) {
            createFile(packageName, "model") {
                generateModels(classDeclaration, autoGeneratedColumns, otherColumns)
            }.writeTo(codeGenerator, aggregating = false)

        }
        if (exposedTable.tableFunctions) {
            createFile(packageName, "functions") {
                generateTableFunctions(classDeclaration, autoGeneratedColumns, otherColumns)

            }.writeTo(codeGenerator, aggregating = false)
        }
    }

    private fun FileSpec.Builder.generateModels(
        tableDeclaration: KSClassDeclaration,
        autoGeneratedColumns: List<KSPropertyDeclaration>,
        otherColumns: List<KSPropertyDeclaration>,
    ) {
        val hasAutoGeneratedColumns = autoGeneratedColumns.isNotEmpty()

        if (hasAutoGeneratedColumns) {
            addInterface(tableDeclaration.createInterfaceClassName) {
                addColumnsAsProperties(otherColumns)
            }
            addClass(tableDeclaration.createDtoClassName) {
                addModifiers(KModifier.DATA)
                addSuperinterface(tableDeclaration.createInterfaceClassName)
                addPrimaryConstructor {
                    addColumnsAsParameters(otherColumns)
                }
                addColumnsAsProperties(otherColumns, parameter = true, override = true)
            }

        }

        addInterface(tableDeclaration.fullInterfaceClassName) {
            if (hasAutoGeneratedColumns) {
                addSuperinterface(tableDeclaration.createInterfaceClassName)
            }
            addColumnsAsProperties(autoGeneratedColumns)
        }
        addClass(tableDeclaration.fullDtoClassName) {
            addModifiers(KModifier.DATA)
            addSuperinterface(tableDeclaration.fullInterfaceClassName)
            addPrimaryConstructor {
                addColumnsAsParameters(autoGeneratedColumns + otherColumns)
            }
            addColumnsAsProperties(autoGeneratedColumns + otherColumns, parameter = true, override = true)
        }
    }

    private fun FileSpec.Builder.generateTableFunctions(
        tableDeclaration: KSClassDeclaration,
        autoGeneratedColumns: List<KSPropertyDeclaration>,
        otherColumns: List<KSPropertyDeclaration>,
    ) {
        val hasAutoGeneratedColumns = autoGeneratedColumns.isNotEmpty()
        val tableClassName = tableDeclaration.toClassName()
        val tableName = tableDeclaration.tableName

        val interfaceClassName = if (hasAutoGeneratedColumns)
            tableDeclaration.createInterfaceClassName
        else
            tableDeclaration.fullInterfaceClassName

        addImport("org.jetbrains.exposed.sql", "insert", "update")

        generateCrudFunctions(
            interfaceClassName = interfaceClassName,
            tableClassName = tableClassName,
            tableName = tableName,
            autoGeneratedColumns = autoGeneratedColumns,
            otherColumns = otherColumns,
        )
        generateMappingFunctions(interfaceClassName, tableName, otherColumns)
    }

    private fun FileSpec.Builder.generateCrudFunctions(
        interfaceClassName: ClassName,
        tableClassName: ClassName,
        tableName: String,
        autoGeneratedColumns: List<KSPropertyDeclaration>,
        otherColumns: List<KSPropertyDeclaration>,
    ) {
        // TODO handle tables with another name for id column
        val idColumn = autoGeneratedColumns.find {
            it.simpleName.asString() == "id"
        }

        fun getUpdateFun(param: String) = idColumn?.let {
            "$tableName.update({ $tableName.id.eq($param) })"
        } ?: "$tableName.update"

        addFunction("insertDto") {
            receiver(tableClassName)
            addParameter("dto", interfaceClassName)
            addCodeBlock {
                beginControlFlow("$tableName.insert")
                addStatement("it.$mappingFunName(dto)")
                endControlFlow()
            }
        }

        addFunction("updateDto") {
            receiver(tableClassName)
            idColumn?.let {
                addColumnsAsParameters(listOf(idColumn))
            }
            addParameter("dto", interfaceClassName)
            addCodeBlock {
                beginControlFlow(getUpdateFun("id"))
                addStatement("it.$mappingFunName(dto)")
                endControlFlow()
            }
        }

        addFunction("insertDto") {
            receiver(tableClassName)
            addColumnsAsParameters(otherColumns)
            addCodeBlock {
                beginControlFlow("$tableName.insert")
                add("it.$mappingFunName(\n")
                otherColumns.forEach { column ->
                    val name = column.simpleName.asString()
                    addStatement("  $name = $name,")
                }
                add(")\n")
                endControlFlow()
            }
        }

        addFunction("updateDto") {
            receiver(tableClassName)
            idColumn?.let {
                addColumnsAsParameters(listOf(idColumn))
            }
            addColumnsAsParameters(otherColumns)
            addCodeBlock {
                beginControlFlow(getUpdateFun("id"))
                add("it.$mappingFunName(\n")
                otherColumns.forEach { column ->
                    val name = column.simpleName.asString()
                    addStatement("  $name = $name,")
                }
                add(")\n")
                endControlFlow()
            }
        }
    }


    private fun FileSpec.Builder.generateMappingFunctions(
        interfaceClassName: ClassName,
        tableName: String,
        otherColumns: List<KSPropertyDeclaration>,
    ) {

        addFunction(mappingFunName) {
            receiver(updateBuilderClassName)
            addParameter("dto", interfaceClassName)

            otherColumns.forEach { column ->
                val name = column.simpleName.asString()
                addStatement("this[$tableName.$name] = dto.$name")
            }
        }

        addFunction(mappingFunName) {
            receiver(updateBuilderClassName)
            addColumnsAsParameters(otherColumns)
            otherColumns.forEach { column ->
                val name = column.simpleName.asString()
                addStatement("this[$tableName.$name] = $name")
            }
        }
    }

    private fun FunSpec.Builder.addColumnsAsParameters(
        columns: List<KSPropertyDeclaration>,
        nullDefault: Boolean = true,
    ) {
        columns.forEach { column ->
            val type = column.type.resolve().getFirstArgumentType().unwrapEntityId()
            val typeName = type.toTypeName()
            addParameter(column.simpleName.asString(), typeName) {
                if (nullDefault && typeName.isNullable) {
                    defaultValue("%L", null)
                }
            }
        }
    }

    private fun TypeSpec.Builder.addColumnsAsProperties(
        columns: List<KSPropertyDeclaration>,
        parameter: Boolean = false,
        override: Boolean = false,
    ) {
        columns.forEach { column ->
            val type = column.type.resolve().getFirstArgumentType().unwrapEntityId()
            addProperty(column.simpleName.asString(), type.toTypeName()) {
                column.docString?.let {
                    addKdoc(it.trim())
                }
                if (parameter) {
                    initializer(column.simpleName.asString())
                }
                if (override) {
                    addModifiers(KModifier.OVERRIDE)
                }
            }
        }
    }

    private fun KSType.unwrapEntityId(): KSType {
        return if (isMatched("org.jetbrains.exposed.dao.id.EntityID")) {
            getFirstArgumentType()
        } else {
            this
        }
    }

    private fun getTableColumns(classDeclaration: KSClassDeclaration): List<KSPropertyDeclaration> {
        return classDeclaration.getAllProperties().filter {
            it.type.resolve().isMatched("org.jetbrains.exposed.sql.Column")
        }.filter {
            it.simpleName.asString() != "autoIncColumn"
        }.sortedBy {
            if (it.simpleName.asString() == "id") 0 else 1
        }.toList()
    }
}
