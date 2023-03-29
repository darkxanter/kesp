package com.github.darkxanter.kesp.annotation

import kotlin.reflect.KClass

/** Table projection */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
public annotation class Projection(
    /**
     * Class for a projection.
     *
     * If it's an interface, then [updateFunction] is implicit set to `true` and [readFunction] is set to `false`
     *
     * **/
    val dataClass: KClass<*>,
    /** Generate update function for a data class */
    val updateFunction: Boolean = false,
    /** Generate read function for a class */
    val readFunction: Boolean = true,
)

//@MustBeDocumented
//@Target(AnnotationTarget.CLASS)
//@Retention(AnnotationRetention.SOURCE)
//public annotation class Projections(vararg val dataClass: KClass<*>)
