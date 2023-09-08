package com.fab1an.kotlinjsonstream.serializer

import com.fab1an.kotlinjsonstream.serializer.KotlinSerializerParameter.KotlinSerializerStandardParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import org.junit.jupiter.api.Test

class CodeGenerationBasicTest {

    @Test
    fun serializeStandardTypes() {

        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyClass"),
                    parameters = mapOf(
                        "intField" to KotlinSerializerStandardParameter(
                            typeName = Int::class.asClassName()
                        ),
                        "boolField" to KotlinSerializerStandardParameter(
                            typeName = Boolean::class.asClassName()
                        ),
                        "stringField" to KotlinSerializerStandardParameter(
                            typeName = String::class.asClassName()
                        )
                    )
                )
            )
        )

        CodeGenerator().createSerializationFileSpecs(info).first().toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import kotlin.Boolean
            import kotlin.Int
            import kotlin.String

            public fun JsonWriter.valueMyClass(obj: MyClass?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("intField").value(obj.intField)
                name("boolField").value(obj.boolField)
                name("stringField").value(obj.stringField)
                endObject()
            }

            public fun JsonReader.nextMyClass(): MyClass {
                var intFieldFound = false
                var intField: Int? = null
                var boolFieldFound = false
                var boolField: Boolean? = null
                var stringFieldFound = false
                var stringField: String? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "intfield" -> {
                            intFieldFound = true
                            intField = nextInt()
                        }
                        "boolfield" -> {
                            boolFieldFound = true
                            boolField = nextBoolean()
                        }
                        "stringfield" -> {
                            stringFieldFound = true
                            stringField = nextString()
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                check(intFieldFound) { "field 'intField' not found" }
                check(boolFieldFound) { "field 'boolField' not found" }
                check(stringFieldFound) { "field 'stringField' not found" }
                val obj = MyClass(
                    intField = intField!!,
                    boolField = boolField!!,
                    stringField = stringField!!
                )
                return obj
            }
            
        """.trimIndent()
    }

    @Test
    fun serializeNullableTypes() {
        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyClass"),
                    parameters = mapOf(
                        "nullableInt" to KotlinSerializerStandardParameter(
                            typeName = Int::class.asClassName(),
                            isMarkedNullable = true
                        )
                    )
                )
            )
        )

        CodeGenerator().createSerializationFileSpecs(info).first().toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import com.fab1an.kotlinjsonstream.nextOrNull
            import kotlin.Int

            public fun JsonWriter.valueMyClass(obj: MyClass?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("nullableInt").value(obj.nullableInt)
                endObject()
            }

            public fun JsonReader.nextMyClass(): MyClass {
                var nullableInt: Int? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "nullableint" -> {
                            nullableInt = nextOrNull(JsonReader::nextInt)
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                val obj = MyClass(
                    nullableInt = nullableInt
                )
                return obj
            }
            
        """.trimIndent()
    }

    @Test
    fun serializeEnums() {

        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyEnum"),
                    isEnum = true
                )
            )
        )

        CodeGenerator().createSerializationFileSpecs(info).first().toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter

            public fun JsonWriter.valueMyEnum(obj: MyEnum?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                value(obj.name)
            }

            public fun JsonReader.nextMyEnum(): MyEnum {
                val enumString = nextString()
                return MyEnum.entries.firstOrNull { it.name.equals(enumString, ignoreCase = true) } ?:
                        error("enumValue '${'$'}enumString' not found")
            }
            
        """.trimIndent()
    }

    @Test
    fun serializeTypesInDifferentFilesAndPackages() {

        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example.packageA", "MyHolder"),
                    parameters = mapOf(
                        "child" to KotlinSerializerStandardParameter(
                            typeName = ClassName("com.example.packageB", "MyHoldee")
                        )
                    )
                )
            )
        )

        val serializerFileSpecs = CodeGenerator().createSerializationFileSpecs(info)
        serializerFileSpecs[0].toString() shouldEqual """
            package com.example.packageA

            import com.example.packageB.MyHoldee
            import com.example.packageB.nextMyHoldee
            import com.example.packageB.valueMyHoldee
            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter

            public fun JsonWriter.valueMyHolder(obj: MyHolder?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("child").valueMyHoldee(obj.child)
                endObject()
            }

            public fun JsonReader.nextMyHolder(): MyHolder {
                var childFound = false
                var child: MyHoldee? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "child" -> {
                            childFound = true
                            child = nextMyHoldee()
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                check(childFound) { "field 'child' not found" }
                val obj = MyHolder(
                    child = child!!
                )
                return obj
            }
            
        """.trimIndent()
    }
}
