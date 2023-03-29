val exposedVersion: String by project
val sqliteVersion: String by project
val postgresqlVersion: String by project
val kdatamapperVersion: String by project
val kotlinxSerializationVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    application
}

dependencies {
    compileOnly(project(":annotations"))
    ksp(project(":processor"))

    compileOnly("io.github.darkxanter:kdatamapper-core:$kdatamapperVersion")
    ksp("io.github.darkxanter:kdatamapper-processor:$kdatamapperVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}

sourceSets.configureEach {
    kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
}

ksp {
    arg("kesp.kotlinxSerialization", "true")
}

application {
    mainClass.set("example.AppKt")
}
tasks {
    jar {
        manifest {
            attributes("Main-Class" to application.mainClass.get())
        }
    }
}
