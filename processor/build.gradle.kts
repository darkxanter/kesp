description = "Processor for Exposed KSP"

val kspVersion: String by project
val kotlinpoetVersion: String by project

plugins {
    id("com.github.darkxanter.library-convention")
}

dependencies {
    implementation(project(":annotations"))
    implementation("com.squareup:kotlinpoet-ksp:$kotlinpoetVersion")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
}

tasks.jar {
    archiveBaseName.set("exposed-ksp-${archiveBaseName.get()}")
}
