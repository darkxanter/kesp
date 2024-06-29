val kspVersion: String by project
val kotlinpoetVersion: String by project
val exposedVersion: String by project

plugins {
    id("com.github.darkxanter.library-convention")
}

description = "Processor for Exposed Kotlin Symbol Processor"

dependencies {
    implementation(project(":annotations"))
    implementation("com.squareup:kotlinpoet-ksp:$kotlinpoetVersion")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    testImplementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.5.0")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

tasks.jar {
    archiveBaseName.set("kesp-${archiveBaseName.get()}")
}
