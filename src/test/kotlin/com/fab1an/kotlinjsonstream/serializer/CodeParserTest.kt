package com.fab1an.kotlinjsonstream.serializer

import com.squareup.kotlinpoet.asClassName
import org.junit.jupiter.api.Test

class CodeParserTest {

    @Test
    fun findBasicTypes() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyClass(val stringField: String, 
                          val intField: Int, 
                          val boolField: Boolean)
        """

        val constructors = CodeParser().parse(kotlinSource).constructors
        constructors.size shouldEqual 1
        constructors.first().name.toString() shouldEqual "com.example.MyClass"
        constructors.first().parameters.size shouldEqual 3

        val stringField = constructors.first().parameters["stringField"]!!
        stringField.typeName.toString() shouldEqual "kotlin.String"
        stringField.isMarkedNullable shouldEqual false
        stringField.arguments shouldEqual emptyList()

        val intField = constructors.first().parameters["intField"]!!
        intField.typeName.toString() shouldEqual "kotlin.Int"
        intField.isMarkedNullable shouldEqual false
        intField.arguments shouldEqual emptyList()

        val boolField = constructors.first().parameters["boolField"]!!
        boolField.typeName.toString() shouldEqual "kotlin.Boolean"
        boolField.isMarkedNullable shouldEqual false
        boolField.arguments shouldEqual emptyList()
    }

    @Test
    fun resolveTypesInOtherPackages() {
        val kotlinSourceA = """
            package com.example

            import com.example.packageA.MyHoldeeA
            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyHolder(val data: MyHoldeeA)
        """

        val kotlinSourceB = """
            package com.example.packageA

            import com.example.packageB.MyHoldeeB
            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyHoldeeA(val data: MyHoldeeB)
        """

        val kotlinSourceC = """
            package com.example.packageB

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyHoldeeB
        """

        val constructors = CodeParser()
            .parse(listOf(kotlinSourceA, kotlinSourceB, kotlinSourceC))
            .constructors

        constructors.size shouldEqual 3
        constructors[0].apply {
            name.toString() shouldEqual "com.example.MyHolder"
            parameters.size shouldEqual 1

            parameters["data"]!!.apply {
                typeName.toString() shouldEqual "com.example.packageA.MyHoldeeA"
            }
        }

        constructors[1].apply {
            name.toString() shouldEqual "com.example.packageA.MyHoldeeA"
            parameters.size shouldEqual 1

            parameters["data"]!!.apply {
                typeName.toString() shouldEqual "com.example.packageB.MyHoldeeB"
            }
        }

        constructors[2].apply {
            name.toString() shouldEqual "com.example.packageB.MyHoldeeB"
            parameters.size shouldEqual 0
        }
    }

    @Test
    fun resolveTypesInSamePackage() {
        val kotlinSourceA = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyHolder(val data: MyHoldee)
        """

        val kotlinSourceB = """
            package com.example

            @Ser
            class MyHoldee
        """

        val constructors = CodeParser()
            .parse(listOf(kotlinSourceA, kotlinSourceB))
            .constructors

        constructors.size shouldEqual 2
        constructors[0].apply {
            name.toString() shouldEqual "com.example.MyHolder"
            parameters.size shouldEqual 1

            parameters["data"]!!.apply {
                typeName.toString() shouldEqual "com.example.MyHoldee"
            }
        }

        constructors[1].apply {
            name.toString() shouldEqual "com.example.MyHoldee"
            parameters.size shouldEqual 0
        }
    }

    @Test
    fun findEnums() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            enum class MyEnum{A, B, C}
        """

        val constructors = CodeParser().parse(kotlinSource).constructors
        constructors.size shouldEqual 1
        constructors.first().name.toString() shouldEqual "com.example.MyEnum"
        constructors.first().isEnum shouldEqual true
        constructors.first().parameters.size shouldEqual 0
    }

    @Test
    fun findNullableTypes() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyClass(val nullableIntField: Int?)
        """

        val constructors = CodeParser().parse(kotlinSource).constructors
        constructors.size shouldEqual 1
        constructors.first().name.toString() shouldEqual "com.example.MyClass"
        constructors.first().parameters.size shouldEqual 1

        val nullableIntField = constructors.first().parameters["nullableIntField"]!!
        nullableIntField.typeName.toString() shouldEqual "kotlin.Int"
        nullableIntField.isMarkedNullable shouldEqual true
        nullableIntField.arguments shouldEqual emptyList()
    }

    @Test
    fun findImportedTypes() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            import com.example.SomethingOther
            
            @Ser
            class MyClass(val otherField: SomethingOther)
        """

        val constructors = CodeParser().parse(kotlinSource).constructors
        constructors.size shouldEqual 1
        constructors.first().name.toString() shouldEqual "com.example.MyClass"
        constructors.first().parameters.size shouldEqual 1

        val otherField = constructors.first().parameters["otherField"]!!
        otherField.typeName.toString() shouldEqual "com.example.SomethingOther"
        otherField.typeName.packageName shouldEqual "com.example"
        otherField.typeName.simpleName shouldEqual "SomethingOther"
        otherField.isMarkedNullable shouldEqual false
        otherField.arguments shouldEqual emptyList()
    }

    @Test
    fun findLists() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyClass(val listField: List<String>)
        """

        val constructors = CodeParser().parse(kotlinSource).constructors
        constructors.size shouldEqual 1
        constructors.first().name.toString() shouldEqual "com.example.MyClass"
        constructors.first().isEnum shouldEqual false
        constructors.first().parameters.size shouldEqual 1

        val listField = constructors.first().parameters["listField"]!!
        listField.typeName.toString() shouldEqual "kotlin.collections.List"
        listField.isMarkedNullable shouldEqual false
        listField.arguments.size shouldEqual 1
        listField.arguments[0].typeName.toString() shouldEqual "kotlin.String"
        listField.arguments[0].isMarkedNullable shouldEqual false
        listField.arguments[0].arguments shouldEqual emptyList()
    }

    @Test
    fun findSets() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyClass(val setField: Set<String>)
        """

        val constructors = CodeParser().parse(kotlinSource).constructors
        constructors.size shouldEqual 1
        constructors.first().name.toString() shouldEqual "com.example.MyClass"
        constructors.first().isEnum shouldEqual false
        constructors.first().parameters.size shouldEqual 1

        val listField = constructors.first().parameters["setField"]!!
        listField.typeName.toString() shouldEqual "kotlin.collections.Set"
        listField.isMarkedNullable shouldEqual false
        listField.arguments.size shouldEqual 1
        listField.arguments[0].typeName.toString() shouldEqual "kotlin.String"
        listField.arguments[0].isMarkedNullable shouldEqual false
        listField.arguments[0].arguments shouldEqual emptyList()
    }

    @Test
    fun findMultipleTypesInFile() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyClass()

            @Ser
            class MyClass2()
            
            class MyClassNotSer()
        """

        val constructors = CodeParser().parse(kotlinSource).constructors
        constructors.size shouldEqual 2
        constructors[0].name.toString() shouldEqual "com.example.MyClass"
        constructors[0].isEnum shouldEqual false
        constructors[0].parameters.size shouldEqual 0

        constructors[1].name.toString() shouldEqual "com.example.MyClass2"
        constructors[1].isEnum shouldEqual false
        constructors[1].parameters.size shouldEqual 0
    }

    @Test
    fun findTypesWithoutBraces() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyClass
        """

        val constructors = CodeParser().parse(kotlinSource).constructors
        constructors.size shouldEqual 1
        constructors[0].name.toString() shouldEqual "com.example.MyClass"
        constructors[0].isEnum shouldEqual false
        constructors[0].parameters.size shouldEqual 0
    }

    @Test
    fun findInterfaces() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyHolder(val obj: MyInterface)

            @Ser
            interface MyInterface {
                fun callMe(): String
            }

            @Ser
            class MyHoldeeA() : MyInterface {
                override fun callMe(): String {
                    return "A"
                }
            }

            @Ser
            class MyHoldeeB() : MyInterface {
                override fun callMe(): String {
                    return "B"
                }
            }
        """

        val results = CodeParser().parse(kotlinSource)

        val constructors = results.constructors
        constructors.size shouldEqual 3
        constructors[0].name.toString() shouldEqual "com.example.MyHolder"
        constructors[0].parameters.size shouldEqual 1
        constructors[0].parameters["obj"]!!.typeName.toString() shouldEqual "com.example.MyInterface"

        constructors[1].name.toString() shouldEqual "com.example.MyHoldeeA"
        constructors[1].parameters.size shouldEqual 0

        constructors[2].name.toString() shouldEqual "com.example.MyHoldeeB"
        constructors[2].parameters.size shouldEqual 0

        val interfaces = results.interfaces
        interfaces.size shouldEqual 1
        interfaces[0].name.toString() shouldEqual "com.example.MyInterface"

        interfaces[0].implementations.size shouldEqual 2
        interfaces[0].implementations[0].toString() shouldEqual "com.example.MyHoldeeA"
        interfaces[0].implementations[1].toString() shouldEqual "com.example.MyHoldeeB"
    }

    @Test
    fun findInterfaceInDifferentPackages() {
        val kotlinSourceA = """
            package com.example.packageA

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            interface MyInterface 

            @Ser
            class MyInterfaceImplA : MyInterface 
        """

        val kotlinSourceB = """
            package com.example.packageB

            import com.example.packageA.MyInterface
            
            @Ser
            class MyInterfaceImplB : MyInterface 
        """

        val info = CodeParser().parse(listOf(kotlinSourceA, kotlinSourceB))

        info.constructors.size shouldEqual 2
        info.constructors[0].apply {
            name.toString() shouldEqual "com.example.packageA.MyInterfaceImplA"
            parameters.size shouldEqual 0
        }
        info.constructors[1].apply {
            name.toString() shouldEqual "com.example.packageB.MyInterfaceImplB"
            parameters.size shouldEqual 0
        }

        info.interfaces.size shouldEqual 1
        info.interfaces[0].apply {
            name.toString() shouldEqual "com.example.packageA.MyInterface"
            implementations.size shouldEqual 2
            implementations[0].toString() shouldEqual "com.example.packageA.MyInterfaceImplA"
            implementations[1].toString() shouldEqual "com.example.packageB.MyInterfaceImplB"
        }
    }

    @Test
    fun findInterfaceOrderDoesNotMatter() {
        val kotlinSourceA = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            interface MyInterface 
        """

        val kotlinSourceB = """
            package com.example

            @Ser
            class MyInterfaceImpl : MyInterface 
        """

        CodeParser().parse(listOf(kotlinSourceA, kotlinSourceB)) shouldEqual
                CodeParser().parse(listOf(kotlinSourceB, kotlinSourceA))
    }

    @Test
    fun allowOtherInterface() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            class MyHolder : MyInterface

            interface MyInterface 
        """

        val results = CodeParser().parse(kotlinSource)

        val constructors = results.constructors
        constructors.size shouldEqual 1
        constructors[0].name.toString() shouldEqual "com.example.MyHolder"
        constructors[0].parameters.size shouldEqual 0

        val interfaces = results.interfaces
        interfaces.size shouldEqual 0
    }

    @Test
    fun parseCircularStructures() {
        val kotlinSource = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            data class MyRoot(var list: List<MyLeaf>)

            @Ser
            data class MyLeaf(@ParentRef val parent: MyRoot, val leafData: Int)
        """

        val results = CodeParser().parse(kotlinSource)

        val constructors = results.constructors
        constructors.size shouldEqual 2
        constructors[0].name.toString() shouldEqual "com.example.MyRoot"
        constructors[0].parameters.size shouldEqual 1
        constructors[0].parameters["list"]!!.typeName shouldEqual List::class.asClassName()
        constructors[0].parameters["list"]!!.arguments.size shouldEqual 1
        constructors[0].parameters["list"]!!.arguments[0].typeName.toString() shouldEqual "com.example.MyLeaf"

        constructors[1].name.toString() shouldEqual "com.example.MyLeaf"
        constructors[1].parameters.size shouldEqual 2
        constructors[1].parameters["parent"]!!.typeName.toString() shouldEqual "com.example.MyRoot"
        constructors[1].parameters["parent"]!!.isParentRef shouldEqual true
        constructors[1].parameters["parent"]!!.arguments.size shouldEqual 0
        constructors[1].parameters["leafData"]!!.typeName.toString() shouldEqual "kotlin.Int"
        constructors[1].parameters["leafData"]!!.isParentRef shouldEqual false
        constructors[1].parameters["leafData"]!!.arguments.size shouldEqual 0
    }

    @Test
    fun findCircularStructuresInDifferentFiles() {
        val kotlinSourceA = """
            package com.example

            import com.fab1an.kotlinserializer.Ser
            
            @Ser
            data class MyRoot(var list: List<MyLeaf>)
        """

        val kotlinSourceB = """
            package com.example
            
            @Ser
            data class MyLeaf(@ParentRef val parent: MyRoot, val leafData: Int)
        """

        val info = CodeParser().parse(listOf(kotlinSourceA, kotlinSourceB))

        info.constructors.size shouldEqual 2
        info.constructors[0].apply {
            name.toString() shouldEqual "com.example.MyRoot"
            parameters.size shouldEqual 1
            parameters["list"]!!.apply {
                isCollection shouldEqual true
                typeName shouldEqual List::class.asClassName()
                arguments.size shouldEqual 1
                arguments[0].apply {
                    typeName.toString() shouldEqual "com.example.MyLeaf"
                    needsParentRef shouldEqual true
                }
            }
        }

        info.constructors[1].apply {
            name.toString() shouldEqual "com.example.MyLeaf"
            parameters.size shouldEqual 2
            parameters["parent"]!!.apply {
                typeName.toString() shouldEqual "com.example.MyRoot"
                isParentRef shouldEqual true
            }
            parameters["leafData"]!!.apply {
                typeName shouldEqual Int::class.asClassName()
            }
        }
    }
}
