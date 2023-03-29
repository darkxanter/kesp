package example.value_class

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.UUIDColumnType
import java.sql.ResultSet
import kotlin.reflect.KClass

public interface ValueId<T : Comparable<T>> : Comparable<ValueId<T>> {
    public val value: T

    override fun compareTo(other: ValueId<T>): Int = value.compareTo(other.value)
}

public inline fun <reified TValue : Comparable<TValue>, reified TWrapperClass : ValueId<TValue>> Column<TValue>.valueId(
    noinline createWrapper: (TValue) -> TWrapperClass,
): Column<TWrapperClass> = wrapperClass(createWrapper) { it.value }

public fun <TValue : Comparable<TValue>, TWrapperClass : ValueId<TValue>> Column<TValue>.valueId(
    createWrapper: (TValue) -> TWrapperClass,
    wrapperClass: KClass<TWrapperClass>,
): Column<TWrapperClass> = wrapperClass(createWrapper, { it.value }, wrapperClass)

public inline fun <reified TValue : Comparable<TValue>, reified TWrapperClass : ValueId<TValue>> valueIdColumnType(
    columnType: IColumnType,
    noinline createWrapper: (TValue) -> TWrapperClass,
): WrapperColumnType<TValue, TWrapperClass> = wrapperColumnType(columnType, createWrapper) { it.value }

public inline fun <reified TValue : Comparable<TValue>, reified TWrapperClass : ValueId<TValue>> valueLongIdColumnType(
    noinline createWrapper: (TValue) -> TWrapperClass,
): WrapperColumnType<TValue, TWrapperClass> = valueIdColumnType(LongColumnType(), createWrapper)

public inline fun <reified TValue : Comparable<TValue>, reified TWrapperClass : ValueId<TValue>> valueIntIdColumnType(
    noinline createWrapper: (TValue) -> TWrapperClass,
): WrapperColumnType<TValue, TWrapperClass> = valueIdColumnType(IntegerColumnType(), createWrapper)

public inline fun <reified TValue : Comparable<TValue>, reified TWrapperClass : ValueId<TValue>> valueUUIDColumnType(
    noinline createWrapper: (TValue) -> TWrapperClass,
): WrapperColumnType<TValue, TWrapperClass> = valueIdColumnType(UUIDColumnType(), createWrapper)

public class WrapperColumnType<TValue : Any, TWrapperClass : Any>(
    public val columnType: IColumnType,
    public val createWrapper: (TValue) -> TWrapperClass,
    public val readValue: (TWrapperClass) -> TValue,
    public val wrapperClass: KClass<TWrapperClass>,
) : ColumnType() {
    override fun sqlType(): String = columnType.sqlType()

    override fun notNullValueToDB(value: Any): Any {
        return columnType.notNullValueToDB(convertToValue(value))
    }

    override fun nonNullValueToString(value: Any): String {
        return columnType.nonNullValueToString(convertToValue(value))
    }

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): TWrapperClass = createWrapper(
        when {
            wrapperClass.isInstance(value) -> readValue(value as TWrapperClass)
            else -> columnType.valueFromDB(value) as TValue
        }
    )

    override fun readObject(rs: ResultSet, index: Int): Any? = columnType.readObject(rs, index)

    override fun hashCode(): Int = 31 * super.hashCode() + columnType.hashCode()

    @Suppress("UNCHECKED_CAST")
    private fun convertToValue(value: Any): Any {
        return when {
            wrapperClass.isInstance(value) -> readValue(value as TWrapperClass)
            else -> value
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as WrapperColumnType<*, *>

        if (columnType != other.columnType) return false
        if (createWrapper != other.createWrapper) return false
        if (readValue != other.readValue) return false
        return wrapperClass == other.wrapperClass
    }
}

public inline fun <reified TValue : Any, reified TWrapperClass : Any> Column<TValue>.wrapperClass(
    noinline createWrapper: (TValue) -> TWrapperClass,
    noinline readValue: (TWrapperClass) -> TValue,
): Column<TWrapperClass> {
    return wrapperClass(createWrapper, readValue, TWrapperClass::class)
}

public fun <TValue : Any, TWrapperClass : Any> Column<TValue>.wrapperClass(
    createWrapper: (TValue) -> TWrapperClass,
    readValue: (TWrapperClass) -> TValue,
    wrapperClass: KClass<TWrapperClass>,
): Column<TWrapperClass> {
    val newColumn = Column<TWrapperClass>(
        table = table,
        name = name,
        columnType = WrapperColumnType(
            columnType = columnType,
            createWrapper = createWrapper,
            readValue = readValue,
            wrapperClass = wrapperClass,
        )
    ).also {
        it.defaultValueFun = defaultValueFun?.let { { createWrapper(it()) } }
    }
    return table.replaceColumn(this, newColumn)
}

public inline fun <reified TValue : Any, reified TWrapperClass : Any> wrapperColumnType(
    columnType: IColumnType,
    noinline createWrapper: (TValue) -> TWrapperClass,
    noinline readValue: (TWrapperClass) -> TValue,
): WrapperColumnType<TValue, TWrapperClass> {
    return WrapperColumnType(
        columnType = columnType,
        createWrapper = createWrapper,
        readValue = readValue,
        wrapperClass = TWrapperClass::class,
    )
}
