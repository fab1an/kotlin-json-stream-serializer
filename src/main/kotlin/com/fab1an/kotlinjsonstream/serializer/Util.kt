package com.fab1an.kotlinjsonstream.serializer

internal fun String.capitalizeAscii(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
