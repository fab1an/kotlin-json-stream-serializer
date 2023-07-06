package com.fab1an.kotlinjsonstream.serializer

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

data class KotlinSerializerInfo(
    val constructors: List<KotlinSerializerConstructorInfo> = emptyList(),
    val interfaces: List<KotlinSerializerInterfaceInfo> = emptyList()
)

data class KotlinSerializerConstructorInfo(
    val name: ClassName,
    val parameters: Map<String, KotlinSerializerParameter> = emptyMap(),
    val isEnum: Boolean = false
) {

    init {
        check(!isEnum || parameters.isEmpty()) { this }
    }
}

data class KotlinSerializerParameter(
    val typeName: ClassName,
    val isMarkedNullable: Boolean = false,
    val arguments: List<KotlinSerializerParameter> = emptyList(),
    var needsParentRef: Boolean = false,
    val isParentRef: Boolean = false
) {

    init {
        if (arguments.isNotEmpty()) {
            check(isCollection) { "unsupported type with arguments: $this" }
            check(!needsParentRef) { "collection cannot need parentRef: $this" }
            check(!isMarkedNullable) { "collection must not be nullable: $this" }
            check(arguments.size == 1) { "collection needs exactly one argument: $this" }
        }
    }

    val isCollection: Boolean
        get() = when (typeName) {
            Set::class.asClassName(), List::class.asClassName() -> true
            else -> false
        }
}

data class KotlinSerializerInterfaceInfo(
    val name: ClassName,
    val implementations: List<ClassName>,
    val commonNeededParentRef: ClassName?
)
