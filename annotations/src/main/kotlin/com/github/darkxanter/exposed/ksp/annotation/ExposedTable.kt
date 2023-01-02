package com.github.darkxanter.exposed.ksp.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class ExposedTable(
    val models: Boolean = true,
    val tableFunctions: Boolean = true,
    val crudRepository: Boolean = true,
)
