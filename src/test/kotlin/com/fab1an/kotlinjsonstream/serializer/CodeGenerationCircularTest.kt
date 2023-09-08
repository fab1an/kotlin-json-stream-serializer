package com.fab1an.kotlinjsonstream.serializer

import com.fab1an.kotlinjsonstream.serializer.KotlinSerializerParameter.KotlinSerializerCollectionParameter
import com.fab1an.kotlinjsonstream.serializer.KotlinSerializerParameter.KotlinSerializerStandardParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import org.junit.jupiter.api.Test

class CodeGenerationCircularTest {

    @Test
    fun serializeCircularStructureWithNullable() {
        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyLeaf"),
                    parameters = mapOf(
                        "parent" to KotlinSerializerStandardParameter(
                            isParentRef = true,
                            typeName = ClassName("com.example", "MyRoot")
                        ),
                        "leafData" to KotlinSerializerStandardParameter(
                            typeName = Int::class.asClassName()
                        )
                    )
                ),
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyRoot"),
                    parameters = mapOf(
                        "myLeaf" to KotlinSerializerStandardParameter(
                            needsParentRef = true,
                            typeName = ClassName("com.example", "MyLeaf"),
                            isMarkedNullable = true
                        )
                    )
                )
            )
        )

        val serializerFileSpecs = CodeGenerator().createSerializationFileSpecs(info)
        serializerFileSpecs[0].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import kotlin.Int

            public fun JsonWriter.valueMyLeaf(obj: MyLeaf?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("leafData").value(obj.leafData)
                endObject()
            }

            public fun JsonReader.nextMyLeaf(): (MyRoot) -> MyLeaf {
                var leafDataFound = false
                var leafData: Int? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "leafdata" -> {
                            leafDataFound = true
                            leafData = nextInt()
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                check(leafDataFound) { "field 'leafData' not found" }
                val obj = { it: MyRoot ->
                    MyLeaf(
                        parent = it,
                        leafData = leafData!!
                    )
                }
                return obj
            }
            
        """.trimIndent()

        serializerFileSpecs[1].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import com.fab1an.kotlinjsonstream.nextOrNull

            public fun JsonWriter.valueMyRoot(obj: MyRoot?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("myLeaf").valueMyLeaf(obj.myLeaf)
                endObject()
            }

            public fun JsonReader.nextMyRoot(): MyRoot {
                var myLeaf: ((MyRoot) -> MyLeaf)? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "myleaf" -> {
                            myLeaf = nextOrNull(JsonReader::nextMyLeaf)
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                val obj = MyRoot(
                    myLeaf = null
                )
                obj.myLeaf = myLeaf?.invoke(obj)
                return obj
            }
            
        """.trimIndent()
    }

    @Test
    fun serializeCircularStructureWithList() {
        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyLeaf"),
                    parameters = mapOf(
                        "parent" to KotlinSerializerStandardParameter(
                            isParentRef = true,
                            typeName = ClassName("com.example", "MyRoot")
                        ),
                        "leafData" to KotlinSerializerStandardParameter(
                            typeName = Int::class.asClassName()
                        )
                    )
                ),
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyRoot"),
                    parameters = mapOf(
                        "myLeafs" to KotlinSerializerCollectionParameter(
                            typeName = List::class.asClassName(),
                            argument = KotlinSerializerStandardParameter(
                                typeName = ClassName("com.example", "MyLeaf"),
                                needsParentRef = true
                            )
                        )
                    )
                )
            )
        )

        val serializerFileSpecs = CodeGenerator().createSerializationFileSpecs(info)

        serializerFileSpecs[0].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import kotlin.Int

            public fun JsonWriter.valueMyLeaf(obj: MyLeaf?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("leafData").value(obj.leafData)
                endObject()
            }

            public fun JsonReader.nextMyLeaf(): (MyRoot) -> MyLeaf {
                var leafDataFound = false
                var leafData: Int? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "leafdata" -> {
                            leafDataFound = true
                            leafData = nextInt()
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                check(leafDataFound) { "field 'leafData' not found" }
                val obj = { it: MyRoot ->
                    MyLeaf(
                        parent = it,
                        leafData = leafData!!
                    )
                }
                return obj
            }
            
        """.trimIndent()

        serializerFileSpecs[1].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import com.fab1an.kotlinjsonstream.`value`
            import com.fab1an.kotlinjsonstream.nextList
            import kotlin.collections.List

            public fun JsonWriter.valueMyRoot(obj: MyRoot?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("myLeafs").`value`(obj.myLeafs, JsonWriter::valueMyLeaf)
                endObject()
            }

            public fun JsonReader.nextMyRoot(): MyRoot {
                var myLeafsFound = false
                var myLeafs: List<(MyRoot) -> MyLeaf>? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "myleafs" -> {
                            myLeafsFound = true
                            myLeafs = nextList(JsonReader::nextMyLeaf)
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                check(myLeafsFound) { "field 'myLeafs' not found" }
                val obj = MyRoot(
                    myLeafs = emptyList()
                )
                obj.myLeafs = myLeafs!!.map { it(obj) }
                return obj
            }
            
        """.trimIndent()
    }

    @Test
    fun serializeCircularStructureWithSet() {
        val info = KotlinSerializerInfo(
            constructors = listOf(
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyLeaf"),
                    parameters = mapOf(
                        "parent" to KotlinSerializerStandardParameter(
                            isParentRef = true,
                            typeName = ClassName("com.example", "MyRoot")
                        )
                    )
                ),
                KotlinSerializerConstructorInfo(
                    name = ClassName("com.example", "MyRoot"),
                    parameters = mapOf(
                        "leafSet" to KotlinSerializerCollectionParameter(
                            typeName = Set::class.asClassName(),
                            argument = KotlinSerializerStandardParameter(
                                typeName = ClassName("com.example", "MyLeaf"),
                                needsParentRef = true
                            )
                        )
                    )
                )
            )
        )

        val serializerFileSpecs = CodeGenerator().createSerializationFileSpecs(info)

        serializerFileSpecs[0].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter

            public fun JsonWriter.valueMyLeaf(obj: MyLeaf?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                endObject()
            }

            public fun JsonReader.nextMyLeaf(): (MyRoot) -> MyLeaf {
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        else -> { skipValue() }
                    }
                }
                endObject()
                val obj = { it: MyRoot ->
                    MyLeaf(
                        parent = it
                    )
                }
                return obj
            }
            
        """.trimIndent()

        serializerFileSpecs[1].toString() shouldEqual """
            package com.example

            import com.fab1an.kotlinjsonstream.JsonReader
            import com.fab1an.kotlinjsonstream.JsonWriter
            import com.fab1an.kotlinjsonstream.`value`
            import com.fab1an.kotlinjsonstream.nextSet
            import kotlin.collections.Set

            public fun JsonWriter.valueMyRoot(obj: MyRoot?) {
                if (obj == null) {
                    nullValue()
                    return
                }
                beginObject()
                name("leafSet").`value`(obj.leafSet, JsonWriter::valueMyLeaf)
                endObject()
            }

            public fun JsonReader.nextMyRoot(): MyRoot {
                var leafSetFound = false
                var leafSet: Set<(MyRoot) -> MyLeaf>? = null
                beginObject()
                while (hasNext()) {
                    when(nextName().lowercase()) {
                        "leafset" -> {
                            leafSetFound = true
                            leafSet = nextSet(JsonReader::nextMyLeaf)
                        }
                        else -> { skipValue() }
                    }
                }
                endObject()
                check(leafSetFound) { "field 'leafSet' not found" }
                val obj = MyRoot(
                    leafSet = emptySet()
                )
                obj.leafSet = leafSet!!.map { it(obj) }.toSet()
                return obj
            }
            
        """.trimIndent()
    }
}
