package com.fab1an.kotlinjsonstream.serializer

import kotlin.test.assertEquals
import kotlin.test.assertTrue

infix fun <T> T.shouldEqual(expected: T) {
    assertEquals(expected, this)
}

inline fun <reified T> Any.shouldBe(block: T.() -> Unit) {
    assertTrue(this is T, "$this")
    this.block()
}
