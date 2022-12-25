package com.github.darkxanter.exposed.annotation

public annotation class ExposedTable(
    val models: Boolean = true,
    val tableFunctions: Boolean = true,
    val crudRepository: Boolean = true,
)
