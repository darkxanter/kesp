package com.github.darkxanter.kesp.annotation

import kotlin.reflect.KClass

/**
 * Define a foreign key
 *
 * Only used to DAO generation
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class ForeignKey(
    val table: KClass<*>,
    val column: String,
)
