package com.github.darkxanter.kesp.annotation

import kotlin.reflect.KClass

/** Table projection */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
public annotation class Projection(val dataClass: KClass<*>)

//@MustBeDocumented
//@Target(AnnotationTarget.CLASS)
//@Retention(AnnotationRetention.SOURCE)
//public annotation class Projections(vararg val dataClass: KClass<*>)
