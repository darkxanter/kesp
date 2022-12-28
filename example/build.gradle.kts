val exposedVersion: String by project

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    application
}

dependencies {
    compileOnly(project(":annotations"))
    ksp(project(":processor"))

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
}

sourceSets.configureEach {
    kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
}
