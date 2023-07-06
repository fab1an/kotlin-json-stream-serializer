package com.fab1an.kotlinjsonstream.serializer

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import org.junit.jupiter.api.Test

class CodeGenerationCollectionsTest {

    @Test
    fun serializeLists() {
        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyClass"),
                    parameters = mapOf(
                        "stringList" to KotlinSerializerParameter(
                            typeName = List::class.asClassName(),
                            arguments = listOf(
                                KotlinSerializerParameter(
                                    typeName = String::class.asClassName()
                                )
                            )
                        ),
                        "objList" to KotlinSerializerParameter(
                            typeName = List::class.asClassName(),
                            arguments = listOf(
                                KotlinSerializerParameter(
                                    typeName = ClassName("com.example", "MyObj")
                                )
                            )
                        )
                    )
                ),
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyObj")
                )
            )
        )

        val serializerFileSpecs = CodeGenerator().createSerializationFileSpecs(info)

        serializerFileSpecs[0].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import com.fab1an.kotlinjsonstream.`value`
            import com.fab1an.kotlinjsonstream.nextList
            import kotlin.String
            import kotlin.collections.List

            public fun JsonWriter.valueMyClass(obj: MyClass?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("stringList").`value`(obj.stringList, JsonWriter::value)
                name("objList").`value`(obj.objList, JsonWriter::valueMyObj)
                endObject()
            }

            public fun JsonReader.nextMyClass(): MyClass {
                var stringListFound = false
                var stringList: List<String>? = null
                var objListFound = false
                var objList: List<MyObj>? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "stringlist" -> {
                            stringListFound = true
                            stringList = nextList(JsonReader::nextString)
                        }
                        "objlist" -> {
                            objListFound = true
                            objList = nextList(JsonReader::nextMyObj)
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                check(stringListFound) { "field 'stringList' not found" }
                check(objListFound) { "field 'objList' not found" }
                val obj = MyClass(
                    stringList = stringList!!,
                    objList = objList!!
                )
                return obj
            }
            
        """.trimIndent()

        serializerFileSpecs[1].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            
            public fun JsonWriter.valueMyObj(obj: MyObj?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                endObject()
            }

            public fun JsonReader.nextMyObj(): MyObj {
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        else -> { skipValue() }
                    }
                }
                endObject()
                val obj = MyObj(
                )
                return obj
            }
            
        """.trimIndent()
    }

    @Test
    fun serializeSets() {
        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyClass"),
                    parameters = mapOf(
                        "stringSet" to KotlinSerializerParameter(
                            typeName = Set::class.asClassName(),
                            arguments = listOf(
                                KotlinSerializerParameter(
                                    typeName = String::class.asClassName()
                                )
                            )
                        )
                    )
                )
            )
        )

        val serializerFileSpecs = CodeGenerator().createSerializationFileSpecs(info)

        serializerFileSpecs.single().toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import com.fab1an.kotlinjsonstream.`value`
            import com.fab1an.kotlinjsonstream.nextSet
            import kotlin.String
            import kotlin.collections.Set

            public fun JsonWriter.valueMyClass(obj: MyClass?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("stringSet").`value`(obj.stringSet, JsonWriter::value)
                endObject()
            }

            public fun JsonReader.nextMyClass(): MyClass {
                var stringSetFound = false
                var stringSet: Set<String>? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "stringset" -> {
                            stringSetFound = true
                            stringSet = nextSet(JsonReader::nextString)
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                check(stringSetFound) { "field 'stringSet' not found" }
                val obj = MyClass(
                    stringSet = stringSet!!
                )
                return obj
            }
            
        """.trimIndent()
    }
}
