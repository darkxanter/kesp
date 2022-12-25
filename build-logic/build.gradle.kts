import java.util.Properties

plugins {
    `kotlin-dsl`
}

val javaVersion = JavaVersion.VERSION_1_8

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    val properties = Properties()
    properties.load(rootDir.parentFile.resolve("gradle.properties").inputStream())
    val kotlinVersion: String by properties
    val dokkaVersion: String by properties

    api("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    api("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = javaVersion.toString()
    }
}
