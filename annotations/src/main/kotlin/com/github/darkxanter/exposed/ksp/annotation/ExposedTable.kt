package com.github.darkxanter.exposed.ksp.annotation

/**
 * Specifies that code generation will be run for the table.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class ExposedTable(
    /** Generate DTO for table. Required for CRUD repository generation. */
    val models: Boolean = true,
    /** Generate extension methods for table. */
    val tableFunctions: Boolean = true,
    /** Generate CRUD repository. */
    val crudRepository: Boolean = true,
)
