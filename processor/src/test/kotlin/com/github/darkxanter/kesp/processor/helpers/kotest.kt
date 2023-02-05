package com.github.darkxanter.kesp.processor.helpers

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun beSubtypeOf(kClass: KClass<*>) = object : Matcher<KClass<*>> {
    override fun test(value: KClass<*>) = MatcherResult(
        value.isSubclassOf(kClass),
        { "Class $value should be subtype of $kClass" },
        { "Class $value should not be subtype of $kClass" }
    )
}
