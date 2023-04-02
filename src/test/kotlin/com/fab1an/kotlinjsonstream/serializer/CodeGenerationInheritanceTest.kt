package com.fab1an.kotlinjsonstream.serializer

import com.squareup.kotlinpoet.ClassName
import org.junit.jupiter.api.Test

class CodeGenerationInheritanceTest {

    @Test
    fun serializeInterfaceHierarchies() {
        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyHolder"),
                    parameters = mapOf(
                        "obj" to KotlinSerializerParameter(
                            typeName = ClassName("com.example", "MyInterface")
                        )
                    )
                ),
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyHoldeeA")
                ),
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyHoldeeB")
                )
            ),
            interfaces = listOf(
                KotlinSerializerInterfaceInfo(
                    name = ClassName("com.example", "MyInterface"),
                    implementations = listOf(
                        ClassName("com.example", "MyHoldeeA"),
                        ClassName("com.example", "MyHoldeeB")
                    ),
                    commonNeededParentRef = null
                )
            )
        )

        val serializationFileSpecs = CodeGenerator().createSerializationFileSpecs(info)
        serializationFileSpecs[3].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import kotlin.Unit
            
            public fun JsonWriter.valueMyInterface(obj: MyInterface?): Unit {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginArray()
                when (obj) {
                    is MyHoldeeA -> {
                        value("com.example.MyHoldeeA")
                        valueMyHoldeeA(obj)
                    }
                    is MyHoldeeB -> {
                        value("com.example.MyHoldeeB")
                        valueMyHoldeeB(obj)
                    }
                    else -> error("no serialisation configured for: ${'$'}obj")
                }
                endArray()
            }
            
            public fun JsonReader.nextMyInterface(): MyInterface {
                beginArray()
                val type = nextString()
                val obj = when (type) {
                    "com.example.MyHoldeeA" -> nextMyHoldeeA()
                    "com.example.MyHoldeeB" -> nextMyHoldeeB()
                    else -> error("unknown type ${'$'}type")
                }
                endArray()
                return obj
            }
            
        """.trimIndent()
    }

    @Test
    fun abstractClassesNeedingParentRef() {
        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "Parent"),
                    parameters = mapOf(
                        "child" to KotlinSerializerParameter(
                            typeName = ClassName("com.example", "MyInterface"),
                            needsParentRef = true,
                            isMarkedNullable = true
                        )
                    )
                ),
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "InterfaceImplA"),
                    parameters = mapOf(
                        "parent" to KotlinSerializerParameter(
                            typeName = ClassName("com.example", "Parent"),
                            isParentRef = true
                        )
                    )
                ),
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "InterfaceImplB"),
                    parameters = mapOf(
                        "parent" to KotlinSerializerParameter(
                            typeName = ClassName("com.example", "Parent"),
                            isParentRef = true
                        )
                    )
                )
            ),
            interfaces = listOf(
                KotlinSerializerInterfaceInfo(
                    name = ClassName("com.example", "MyInterface"),
                    implementations = listOf(
                        ClassName("com.example", "InterfaceImplA"),
                        ClassName("com.example", "InterfaceImplB")
                    ),
                    commonNeededParentRef = ClassName("com.example", "Parent")
                )
            )
        )

        val serializationFileSpecs = CodeGenerator().createSerializationFileSpecs(info)
        serializationFileSpecs[3].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import kotlin.Unit
            
            public fun JsonWriter.valueMyInterface(obj: MyInterface?): Unit {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginArray()
                when (obj) {
                    is InterfaceImplA -> {
                        value("com.example.InterfaceImplA")
                        valueInterfaceImplA(obj)
                    }
                    is InterfaceImplB -> {
                        value("com.example.InterfaceImplB")
                        valueInterfaceImplB(obj)
                    }
                    else -> error("no serialisation configured for: ${'$'}obj")
                }
                endArray()
            }
            
            public fun JsonReader.nextMyInterface(): (Parent) -> MyInterface {
                beginArray()
                val type = nextString()
                val obj = when (type) {
                    "com.example.InterfaceImplA" -> nextInterfaceImplA()
                    "com.example.InterfaceImplB" -> nextInterfaceImplB()
                    else -> error("unknown type ${'$'}type")
                }
                endArray()
                return obj
            }
            
        """.trimIndent()
    }
}
