package example.database

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

class JsonColumnType<T : Any>(
    private val json: Json,
    private val serializer: KSerializer<T>,
) : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()

    override fun valueFromDB(value: Any) = when (val v = super.valueFromDB(value)) {
        is String -> json.decodeFromString(serializer, v)
        else -> v
    }

    override fun notNullValueToDB(value: Any): Any = nonNullValueToString(value)

    @Suppress("UNCHECKED_CAST")
    override fun nonNullValueToString(value: Any) = json.encodeToString(serializer, value as T)

    override fun valueToString(value: Any?): String = when (value) {
        is Iterable<*> -> nonNullValueToString(value)
        else -> super.valueToString(value)
    }
}

inline fun <reified T : Any> Table.json(name: String, json: Json = Json): Column<T> = json(name, serializer(), json)

fun <T : Any> Table.json(name: String, serializer: KSerializer<T>, json: Json = Json) =
    registerColumn<T>(name, JsonColumnType(json, serializer))
