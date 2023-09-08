package com.fab1an.kotlinjsonstream.serializer

import com.fab1an.kotlinjsonstream.serializer.KotlinSerializerParameter.KotlinSerializerStandardParameter
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
        check(!isEnum || parameters.isEmpty()) { "enum cannot need parameters: $this" }
        check(parameters.values.count { it is KotlinSerializerStandardParameter && it.isParentRef } in (0..1)) {
            "$this must have 0 or 1 parent-references"
        }
    }

    val neededParentRef: KotlinSerializerStandardParameter?
        get() = parameters.values
            .filterIsInstance<KotlinSerializerStandardParameter>()
            .singleOrNull { it.isParentRef }
}


sealed class KotlinSerializerParameter {
    abstract val typeName: ClassName

    val isCollection: Boolean
        get() = when (typeName) {
            Set::class.asClassName(), List::class.asClassName() -> true
            else -> false
        }

    data class KotlinSerializerStandardParameter(
        override val typeName: ClassName,
        val isMarkedNullable: Boolean = false,
        var needsParentRef: Boolean = false,
        val isParentRef: Boolean = false
    ) : KotlinSerializerParameter() {

        init {
            check(!isCollection) { "$typeName is a collection" }
        }
    }

    data class KotlinSerializerCollectionParameter(
        override val typeName: ClassName,
        val argument: KotlinSerializerParameter
    ) : KotlinSerializerParameter() {

        init {
            check(isCollection) { "$typeName is not a collection" }
        }
    }
}

data class KotlinSerializerInterfaceInfo(
    val name: ClassName,
    val implementations: List<ClassName>,
    val commonNeededParentRef: ClassName?
)
