# 0.13.0

### Improvements
- Support for custom id tables

# 0.12.1

Fix migration to new query DSL


# 0.12.0

### Breaking changes

The minimum supported version of Exposed is now 0.46.0.

### Improvements
- Update Kotlin to 1.9.22
- Support for providing a database instance to the CRUD repository

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
