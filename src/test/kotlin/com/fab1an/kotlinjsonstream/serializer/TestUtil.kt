package com.fab1an.kotlinjsonstream.serializer

import kotlin.test.assertEquals

infix fun <T> T.shouldEqual(expected: T) {
    assertEquals(expected, this)
}
