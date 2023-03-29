package example.value_class

import org.jetbrains.exposed.dao.id.IdTable
import java.util.UUID
import kotlin.reflect.KClass

open class ValueIntIdTable<TWrapperClass : ValueId<Int>>(
    name: String,
    createWrapper: (Int) -> TWrapperClass,
    wrapperClass: KClass<TWrapperClass>,
) : IdTable<TWrapperClass>(name) {
    final override val id = integer("id").valueId(createWrapper, wrapperClass).autoIncrement().entityId()
    final override val primaryKey = PrimaryKey(id)
}

open class ValueLongIdTable<TWrapperClass : ValueId<Long>>(
    name: String,
    createWrapper: (Long) -> TWrapperClass,
    wrapperClass: KClass<TWrapperClass>,
) : IdTable<TWrapperClass>(name) {
    final override val id = long("id").valueId(createWrapper, wrapperClass).autoIncrement().entityId()
    final override val primaryKey = PrimaryKey(id)
}

open class ValueUUIDTable<TWrapperClass : ValueId<UUID>>(
    name: String,
    createWrapper: (UUID) -> TWrapperClass,
    wrapperClass: KClass<TWrapperClass>,
) : IdTable<TWrapperClass>(name) {
    final override val id = uuid("id").valueId(createWrapper, wrapperClass).entityId()
    final override val primaryKey = PrimaryKey(id)
}
