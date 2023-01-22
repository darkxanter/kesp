val kspVersion: String by project
val kotlinpoetVersion: String by project

plugins {
    id("com.github.darkxanter.library-convention")
}

description = "Processor for Exposed Kotlin Symbol Processor"

dependencies {
    implementation(project(":annotations"))
    implementation("com.squareup:kotlinpoet-ksp:$kotlinpoetVersion")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
}

tasks.jar {
    archiveBaseName.set("kesp-${archiveBaseName.get()}")
}
