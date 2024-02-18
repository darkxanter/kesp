# 0.11.0

### Breaking changes

Such extension functions will no longer be created due to a conflict between `fromDto` and other tables with the same type signature:
```kotlin
public fun UserTable.insertDto(
    username: String,
    password: String,
    birthDate: LocalDate? = null,
)

public fun UserTable.updateDto(
    id: Long,
    username: String,
    password: String,
    birthDate: LocalDate? = null,
)

public fun UpdateBuilder<*>.fromDto(
    username: String,
    password: String,
    birthDate: LocalDate? = null,
): Unit {
    this[UserTable.username] = username
    this[UserTable.password] = password
    this[UserTable.birthDate] = birthDate
}
```
