val exposedVersion: String by project
val sqliteVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    application
}

dependencies {
    compileOnly(project(":annotations"))
    ksp(project(":processor"))

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}

sourceSets.configureEach {
    kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
}

ksp {
    arg("kesp.kotlinxSerialization", "true")
}
