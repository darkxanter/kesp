plugins {
    id("com.github.darkxanter.library-convention")
}

description = "Annotations for Kesp Exposed Kotlin Symbol Processor"

tasks.jar {
    archiveBaseName.set("kesp-${archiveBaseName.get()}")
}
