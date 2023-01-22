package com.github.darkxanter

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `java-library`
    `maven-publish`
    signing
}

val javaVersion = JavaVersion.VERSION_1_8

java {
    withSourcesJar()
//    withJavadocJar()
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin {
    explicitApi()
}

dependencies {
//    implementation(platform(kotlin("bom")))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion.toString()
    }

    test {
        useJUnitPlatform()
        testLogging {
            events(
                org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
            )
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

publishing {
    publications {
        create<MavenPublication>("mavenCentral") {
            artifactId = "kesp-$artifactId"
            from(components["java"])
            artifact(javadocJar)

            pom {
                name.set("Kesp")
                afterEvaluate {
                    this@pom.description.set(project.description)
                }
                url.set("https://github.com/darkxanter/kesp")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }
                scm {
                    url.set("https://github.com/darkxanter/kesp")
                    connection.set("scm:git:git://github.com/darkxanter/kesp.git")
                    developerConnection.set("scm:git:git@github.com:darkxanter/kesp.git")
                }
                developers {
                    developer {
                        name.set("Sergey Shumov")
                        email.set("sergey0001@gmail.com")
                    }
                }
            }
        }
    }
}

signing {
    setRequired { !project.version.toString().endsWith("-SNAPSHOT") && !project.hasProperty("skipSigning") }
    if (project.hasProperty("signingKey")) {
        useInMemoryPgpKeys(properties["signingKey"].toString(), properties["signingPassword"].toString())
    } else {
        useGpgCmd()
    }
    sign(publishing.publications["mavenCentral"])
}
