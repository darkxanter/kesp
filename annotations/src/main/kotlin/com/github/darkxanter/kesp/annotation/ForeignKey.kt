package com.github.darkxanter.kesp.annotation

import kotlin.reflect.KClass

/**
 * Define a foreign key
 *
 * Only used to DAO generation
 */
@MustBeDocumented
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class ForeignKey(val table: KClass<*>)
