# Exposed KSP
[![Maven Central](https://img.shields.io/maven-central/v/io.github.darkxanter.exposed/exposed-ksp-annotations)](https://search.maven.org/artifact/io.github.darkxanter.exposed/exposed-ksp-annotations)

**Exposed KSP** is Kotlin Symbol Processor for [Exposed SQL DSL](https://github.com/JetBrains/Exposed/wiki/DSL).


## Example

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

    val birthDate = datetime("birth_date").nullable()
}
```
### Result

<details>
<summary>Models</summary>

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

    public val birthDate: LocalDateTime?
}

public data class UserTableCreateDto(
    /**
     * Username
     */
    public override val username: String,
    /**
     * password
     */
    public override val password: String,
    public override val birthDate: LocalDateTime? = null,
) : UserTableCreate

public interface UserTableFull : UserTableCreate {
    public val id: Long
}

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
    public override val birthDate: LocalDateTime? = null,
) : UserTableFull
```
</details>

<details>
<summary>Functions</summary>

```kotlin
public fun UserTable.insertDto(dto: UserTableCreate): Unit {
    UserTable.insert {
        it.fromDto(dto)
    }
}

public fun UserTable.updateDto(id: Long, dto: UserTableCreate): Unit {
    UserTable.update({ UserTable.id.eq(id) }) {
        it.fromDto(dto)
    }
}

public fun UserTable.insertDto(
    username: String,
    password: String,
    birthDate: LocalDateTime? = null,
): Unit {
    UserTable.insert {
        it.fromDto(
            username = username,
            password = password,
            birthDate = birthDate,
        )
    }
}

public fun UserTable.updateDto(
    id: Long,
    username: String,
    password: String,
    birthDate: LocalDateTime? = null,
): Unit {
    UserTable.update({ UserTable.id.eq(id) }) {
        it.fromDto(
            username = username,
            password = password,
            birthDate = birthDate,
        )
    }
}

public fun ResultRow.toUserTableFullDto(): UserTableFullDto = UserTableFullDto(
    id = this[UserTable.id].value,
    username = this[UserTable.username],
    password = this[UserTable.password],
    birthDate = this[UserTable.birthDate],
)

public fun UpdateBuilder<Any>.fromDto(dto: UserTableCreate): Unit {
    this[UserTable.username] = dto.username
    this[UserTable.password] = dto.password
    this[UserTable.birthDate] = dto.birthDate
}

public fun UpdateBuilder<Any>.fromDto(
    username: String,
    password: String,
    birthDate: LocalDateTime? = null,
): Unit {
    this[UserTable.username] = username
    this[UserTable.password] = password
    this[UserTable.birthDate] = birthDate
}
```
</details>

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
Add `exposed-ksp` dependencies:
```kotlin
dependencies {
    compileOnly("io.github.darkxanter.exposed:exposed-ksp-annotations:0.1.1")
    ksp("io.github.darkxanter.exposed:exposed-ksp-processor:0.1.1")
}
```
To access generated code from KSP, you need to set up the source path into your module's `build.gradle.kts` file:
```kotlin
sourceSets.configureEach {
    kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
}
```
