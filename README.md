# Kesp
[![Maven Central](https://img.shields.io/maven-central/v/io.github.darkxanter.exposed/kesp-annotations)](https://search.maven.org/artifact/io.github.darkxanter.exposed/kesp-annotations)

**Kesp** is Kotlin Symbol Processor for [Exposed SQL DSL](https://github.com/JetBrains/Exposed/wiki/DSL).
It generates for you DTOs, table mappings and a CRUD repository for a Exposed table.

## Features

- generates table mappings and functions
- generates data classes and interfaces
- generates a CRUD repository
- copies KDoc from columns to data class fields
- you can use any custom columns, unlike libraries where you define a data class
and only a table with supported build-in columns is generated

## Example

Given a simple `UserTable`:
```kotlin
@ExposedTable
object UserTable : LongIdTable("users") {
    /**
     * Username
     */
    val username = varchar("username", 255)

    /**
     * password
     */
    val password = varchar("password", 255)

    val birthDate = date("birth_date").nullable()

    val profile = json<UserProfile>("profile")

    @GeneratedValue
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

@Serializable
data class UserProfile(
    val value1: String,
    val value2: Int,
)
```

When we build the project we'll have:

<details>
<summary>DTOs</summary>

```kotlin
public interface UserTableCreate {
    /**
     * Username
     */
    public val username: String

    /**
     * password
     */
    public val password: String

    public val birthDate: LocalDate?

    public val profile: UserProfile
}

@Serializable
public data class UserTableCreateDto(
    /**
     * Username
     */
    public override val username: String,
    /**
     * password
     */
    public override val password: String,
    public override val birthDate: LocalDate? = null,
    public override val profile: UserProfile,
) : UserTableCreate

public interface UserTableFull : UserTableCreate {
    public val id: Long

    public val createdAt: Instant
}

@Serializable
public data class UserTableFullDto(
    public override val id: Long,
    /**
     * Username
     */
    public override val username: String,
    /**
     * password
     */
    public override val password: String,
    public override val birthDate: LocalDate? = null,
    public override val profile: UserProfile,
    public override val createdAt: Instant,
) : UserTableFull
```
</details>

<details>
<summary>Table functions</summary>

```kotlin
public fun UserTable.insertDto(dto: UserTableCreate): Long = UserTable.insertAndGetId {
    it.fromDto(dto)
}.value

public fun UserTable.updateDto(id: Long, dto: UserTableCreate): Int =
    UserTable.update({UserTable.id.eq(id)}) {
        it.fromDto(dto)
    }

public fun UserTable.insertDto(
    username: String,
    password: String,
    birthDate: LocalDate? = null,
    profile: UserProfile,
): Long = UserTable.insertAndGetId {
    it.fromDto(
        username = username,
        password = password,
        birthDate = birthDate,
        profile = profile,
    )
}.value

public fun UserTable.updateDto(
    id: Long,
    username: String,
    password: String,
    birthDate: LocalDate? = null,
    profile: UserProfile,
): Int = UserTable.update({UserTable.id.eq(id)}) {
    it.fromDto(
        username = username,
        password = password,
        birthDate = birthDate,
        profile = profile,
    )
}

public fun ResultRow.toUserTableFullDto(): UserTableFullDto = UserTableFullDto(
    id = this[UserTable.id].value,
    username = this[UserTable.username],
    password = this[UserTable.password],
    birthDate = this[UserTable.birthDate],
    profile = this[UserTable.profile],
    createdAt = this[UserTable.createdAt],
)

public fun ResultRow.toUserTableFullDto(alias: Alias<UserTable>): UserTableFullDto =
    UserTableFullDto(
        id = this[alias[UserTable.id]].value,
        username = this[alias[UserTable.username]],
        password = this[alias[UserTable.password]],
        birthDate = this[alias[UserTable.birthDate]],
        profile = this[alias[UserTable.profile]],
        createdAt = this[alias[UserTable.createdAt]],
    )

public fun Iterable<ResultRow>.toUserTableFullDtoList(): List<UserTableFullDto> = map {
    it.toUserTableFullDto()
}

public fun Iterable<ResultRow>.toUserTableFullDtoList(alias: Alias<UserTable>):
    List<UserTableFullDto> = map {
    it.toUserTableFullDto(alias)
}

public fun UpdateBuilder<*>.fromDto(dto: UserTableCreate): Unit {
    this[UserTable.username] = dto.username
    this[UserTable.password] = dto.password
    this[UserTable.birthDate] = dto.birthDate
    this[UserTable.profile] = dto.profile
}

public fun UpdateBuilder<*>.fromDto(
    username: String,
    password: String,
    birthDate: LocalDate? = null,
    profile: UserProfile,
): Unit {
    this[UserTable.username] = username
    this[UserTable.password] = password
    this[UserTable.birthDate] = birthDate
    this[UserTable.profile] = profile
}
```
</details>

<details>
<summary>Repository</summary>

```kotlin
public open class UserTableRepository {
    public fun find(`where`: (SqlExpressionBuilder.() -> Op<Boolean>)? = null):
        List<UserTableFullDto> {

        return transaction {
            if (where != null) {
                UserTable.select(where).toUserTableFullDtoList()
            } else {
                UserTable.selectAll().toUserTableFullDtoList()
            }
        }
    }

    public fun findOne(`where`: SqlExpressionBuilder.() -> Op<Boolean>): UserTableFullDto? {

        return find(where).singleOrNull()
    }

    public fun findById(id: Long): UserTableFullDto? {

        return findOne {
            UserTable.id.eq(id)
        }
    }

    public fun create(dto: UserTableCreate): Long = transaction {
        UserTable.insertDto(dto)
    }

    public fun update(id: Long, dto: UserTableCreate): Int = transaction {
        UserTable.updateDto(id, dto)
    }

    public fun deleteById(id: Long): Int {

        return delete {
            UserTable.id.eq(id)
        }
    }

    public fun delete(`where`: UserTable.(ISqlExpressionBuilder) -> Op<Boolean>): Int {

        return transaction {
            UserTable.deleteWhere {
                where(it)
            }
        }
    }
}
```
</details>

You can find a complete project example in the `example` subdirectory.

## Gradle setup

Add KSP plugin to your module's `build.gradle.kts`:
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.7.22-1.0.8"
}
```
Add `Maven Central` to the repositories blocks in your project's `build.gradle.kts`:
```kotlin
repositories {
    mavenCentral()
}
```
Add `kesp` dependencies:
```kotlin
dependencies {
    compileOnly("io.github.darkxanter.exposed:kesp-annotations:0.6.0")
    ksp("io.github.darkxanter.exposed:kesp-processor:0.6.0")
}
```
To access generated code from KSP, you need to set up the source path into your module's `build.gradle.kts` file:
```kotlin
sourceSets.configureEach {
    kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
}
```

To create DTO with the `kotlinx.serialization.Serializable` annotation, add to `build.gradle.kts`:
```kotlin
ksp {
    arg("kesp.kotlinxSerialization", "true")
}
```
