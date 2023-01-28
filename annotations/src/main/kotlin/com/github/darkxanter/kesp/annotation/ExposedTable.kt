package com.github.darkxanter.kesp.annotation

/**
 * Specifies that code generation will be run for the table.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class ExposedTable(
    /** Generate DTOs for a table */
    val models: Boolean = true,
    /**
     * Generate extension methods:
     * - `insertDto` and `updateDto` for a table
     * - "to" mappings for `ResultRow`
     * - `fromDto` mappings for `UpdateBuilder`
     * */
    val tableFunctions: Boolean = true,
    /** Generate CRUD repository. */
    val crudRepository: Boolean = true,
)
