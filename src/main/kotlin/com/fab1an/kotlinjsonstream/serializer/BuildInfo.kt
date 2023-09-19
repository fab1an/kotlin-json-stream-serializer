package com.fab1an.kotlinjsonstream.serializer

import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources

object BuildInfo {
    val VERSION: String =
        this.loadPropertyFromResources("com/fab1an/kotlinjsonstream/serializer/build.properties", "version")
            .trim()
}
