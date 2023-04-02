package com.fab1an.kotlinjsonstream.serializer

import com.fab1an.kotlinjsonstream.JsonReader
import com.fab1an.kotlinjsonstream.JsonWriter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName


internal class CodeGenerator {

    fun createSerializationFileSpecs(serializerInfo: KotlinSerializerInfo): List<FileSpec> {
        val files = mutableListOf<FileSpec>()

        /* create new files in correct package */
        serializerInfo.constructors.forEach {
            files += createConstructorSerializerFileSpec(it)
        }

        /* create interfaces */
        serializerInfo.interfaces.forEach {
            files += createInterfaceSerializerFileSpec(it, serializerInfo)
        }

        return files
    }

    private fun createInterfaceSerializerFileSpec(
        interfaceInfo: KotlinSerializerInterfaceInfo,
        serializerInfo: KotlinSerializerInfo
    ): FileSpec {
        return FileSpec.builder(interfaceInfo.name.packageName, interfaceInfo.name.simpleName + "Serialization")
            .addFunction(interfaceSerializer(interfaceInfo))
            .addFunction(interfaceDeserializer(interfaceInfo.name, serializerInfo))
            .indent("    ")
            .build()
    }

    private fun createConstructorSerializerFileSpec(constructor: KotlinSerializerConstructorInfo): FileSpec {
        val className = constructor.name

        return FileSpec.builder(className.packageName, className.simpleName + "Serialization")
            .apply {
                when {
                    constructor.isEnum -> {
                        addFunction(enumSerializer(className))
                        addFunction(enumDeserializer(className))
                    }
                    else -> {
                        addFunction(serializer(constructor))
                        addFunction(deserializer(constructor))
                    }
                }
            }
            .indent("    ")
            .build()
    }

    private fun serializer(info: KotlinSerializerConstructorInfo): FunSpec {
        return FunSpec.builder("value${info.name.simpleName}")
            .receiver(JsonWriter::class)
            .addParameter("obj", info.name.copy(nullable = true))
            .beginControlFlow("if (obj == null) {")
            .addStatement("nullValue()")
            .addStatement("return")
            .endControlFlow()
            .addStatement("beginObject()")
            .apply {
                info.parameters.forEach { (memberName, typeInfo) ->
                    when {
                        typeInfo.isParentRef -> {
                        }
                        typeInfo.arguments.isEmpty() -> {
                            when (typeInfo.typeName) {
                                Int::class.asClassName() -> addStatement("name(%S).value(obj.$memberName)", memberName)
                                String::class.asClassName() -> addStatement(
                                    "name(%S).value(obj.$memberName)",
                                    memberName
                                )
                                Boolean::class.asClassName() -> addStatement(
                                    "name(%S).value(obj.$memberName)",
                                    memberName
                                )
                                Double::class.asClassName() -> addStatement(
                                    "name(%S).value(obj.$memberName)",
                                    memberName
                                )
                                else -> addStatement(
                                    "name(%S).%M(obj.$memberName)",
                                    memberName,
                                    typeInfo.typeName.getValueFunction()
                                )
                            }
                        }
                        else -> {
                            /* collections */
                            val listValueFn = MemberName("com.fab1an.kotlinjsonstream", "value")

                            when (val listArgType = typeInfo.arguments.first().typeName) {
                                Int::class.asClassName() -> addStatement(
                                    "name(%S).%M(obj.$memberName, JsonWriter::value)",
                                    memberName,
                                    listValueFn
                                )
                                String::class.asClassName() -> addStatement(
                                    "name(%S).%M(obj.$memberName, JsonWriter::value)",
                                    memberName,
                                    listValueFn
                                )
                                Boolean::class.asClassName() -> addStatement(
                                    "name(%S).%M(obj.$memberName, JsonWriter::value)",
                                    memberName,
                                    listValueFn
                                )
                                Double::class.asClassName() -> addStatement(
                                    "name(%S).%M(obj.$memberName, JsonWriter::value)",
                                    memberName,
                                    listValueFn
                                )
                                else -> addStatement(
                                    "name(%S).%M(obj.$memberName, JsonWriter::%M)",
                                    memberName,
                                    listValueFn,
                                    listArgType.getValueFunction()
                                )
                            }
                        }
                    }
                }
            }
            .addStatement("endObject()")
            .build()
    }

    private fun deserializer(info: KotlinSerializerConstructorInfo): FunSpec {
        val className = info.name

        check(info.parameters.values.count { it.isParentRef } in (0..1)) { "${info.name} must have 0 or 1 ParentRefs" }
        val needsParentRef = info.parameters.values.any { it.isParentRef }

        return FunSpec.builder("next${className.simpleName.capitalizeAscii()}")
            .receiver(JsonReader::class)
            .apply {
                if (needsParentRef) {
                    val parentRefType = info.parameters.values.single { it.isParentRef }.typeName
                    returns(
                        LambdaTypeName.get(
                            parameters = listOf(ParameterSpec("", parentRefType)),
                            returnType = className
                        )
                    )
                } else {
                    returns(className)
                }
            }
            .apply {
                /* variables */
                info.parameters.forEach { (name, typeInfo) ->
                    when {
                        typeInfo.isParentRef -> {
                        }
                        typeInfo.needsParentRef -> {
                            check(typeInfo.isMarkedNullable) {
                                "field '$name' must be nullable or a List, since ${typeInfo.typeName} needs a parent-ref"
                            }
                            addStatement("var ${name}Found = false")
                            addStatement("var $name: ((%T) -> %T)? = null", info.name, typeInfo.typeName)
                        }
                        typeInfo.isCollection -> {
                            if (typeInfo.arguments.single().needsParentRef) {
                                addStatement("var ${name}Found = false")
                                addStatement(
                                    "var $name: %T<(%T) -> %T>? = null",
                                    typeInfo.typeName,
                                    info.name,
                                    typeInfo.arguments.single().typeName
                                )

                            } else {
                                addStatement("var ${name}Found = false")
                                addStatement(
                                    "var $name: %T<%T>? = null",
                                    typeInfo.typeName,
                                    typeInfo.arguments.single().typeName
                                )
                            }
                        }
                        else -> {
                            addStatement("var ${name}Found = false")
                            addStatement("var $name: %T? = null", typeInfo.typeName)
                        }
                    }
                }
            }
            .addStatement("beginObject()")
            .beginControlFlow("while (hasNext())")
            .beginControlFlow("when(nextName().lowercase()) {")
            .apply {
                info.parameters.forEach { (memberName, typeInfo) ->
                    when {
                        typeInfo.isParentRef -> {
                        }
                        else -> {
                            beginControlFlow("%S -> {", memberName.lowercase())
                            addStatement("${memberName}Found = true")
                            when {
                                typeInfo.isCollection -> {
                                    val nextFn = when (typeInfo.typeName) {
                                        List::class.asClassName() -> MemberName(
                                            "com.fab1an.kotlinjsonstream",
                                            "nextList"
                                        )
                                        Set::class.asClassName() -> MemberName("com.fab1an.kotlinjsonstream", "nextSet")
                                        else -> error("unsupported collection $typeInfo")
                                    }

                                    when (val listArgType = typeInfo.arguments.first().typeName) {
                                        Int::class.asClassName() -> addStatement(
                                            "$memberName = %M(JsonReader::nextInt)",
                                            nextFn
                                        )
                                        String::class.asClassName() -> addStatement(
                                            "$memberName = %M(JsonReader::nextString)",
                                            nextFn
                                        )
                                        Boolean::class.asClassName() -> addStatement(
                                            "$memberName = %M(JsonReader::nextBoolean)",
                                            nextFn
                                        )
                                        Double::class.asClassName() -> addStatement(
                                            "$memberName = %M(JsonReader::nextDouble)",
                                            nextFn
                                        )
                                        else -> addStatement(
                                            "$memberName = %M(JsonReader::%M)",
                                            nextFn,
                                            listArgType.getJsonNextFunction()
                                        )
                                    }
                                }
                                typeInfo.isMarkedNullable -> {
                                    val nextOrNullFn = MemberName("com.fab1an.kotlinjsonstream", "nextOrNull")
                                    when (typeInfo.typeName) {
                                        Int::class.asClassName() -> addStatement(
                                            "$memberName = %M(JsonReader::nextInt)",
                                            nextOrNullFn
                                        )
                                        String::class.asClassName() -> addStatement(
                                            "$memberName = %M(JsonReader::nextString)",
                                            nextOrNullFn
                                        )
                                        Boolean::class.asClassName() -> addStatement(
                                            "$memberName = %M(JsonReader::nextBoolean)",
                                            nextOrNullFn
                                        )
                                        Double::class.asClassName() -> addStatement(
                                            "$memberName = %M(JsonReader::nextDouble)",
                                            nextOrNullFn
                                        )
                                        else -> addStatement(
                                            "$memberName = %M(JsonReader::%M)",
                                            nextOrNullFn,
                                            typeInfo.typeName.getJsonNextFunction()
                                        )
                                    }
                                }
                                typeInfo.arguments.isEmpty() -> {
                                    when (typeInfo.typeName) {
                                        Int::class.asClassName() -> addStatement("$memberName = nextInt()")
                                        String::class.asClassName() -> addStatement("$memberName = nextString()")
                                        Boolean::class.asClassName() -> addStatement("$memberName = nextBoolean()")
                                        Double::class.asClassName() -> addStatement("$memberName = nextDouble()")
                                        else -> addStatement(
                                            "$memberName = %M()",
                                            typeInfo.typeName.getJsonNextFunction()
                                        )
                                    }
                                }
                            }
                            endControlFlow()
                        }
                    }
                }
                addStatement("else -> { skipValue() }")
            }
            .endControlFlow()
            .endControlFlow()
            .addStatement("endObject()")
            .apply {
                /* foundChecks */
                info.parameters.forEach { (memberName, typeInfo) ->
                    if (!typeInfo.isParentRef && !typeInfo.isMarkedNullable)
                        addStatement("""check(${memberName}Found) { "field '$memberName' not found" }""")
                }
            }
            .apply {
                if (needsParentRef) {
                    val parentRef = info.parameters.values.single { it.isParentRef }.typeName
                    beginControlFlow("val obj = { it: %T ->", parentRef)
                    addStatement("%T(", className)
                } else {
                    addStatement("val obj = %T(", className)
                }
            }
            .apply {
                info.parameters.entries.forEachIndexed { idx, (name, type) ->

                    var statement = "    "
                    when {
                        type.needsParentRef -> {
                            statement += "$name = null"
                        }
                        type.isParentRef -> {
                            statement += "$name = it"
                        }
                        type.isCollection && type.arguments.single().needsParentRef -> {
                            statement += when (type.typeName) {
                                Set::class.asClassName() -> "$name = emptySet()"
                                List::class.asClassName() -> "$name = emptyList()"
                                else -> error("unsupported collection $type")
                            }
                        }
                        else -> {
                            statement += "$name = $name"
                            if (!type.isMarkedNullable)
                                statement += "!!"
                        }
                    }

                    if (idx != info.parameters.entries.toList().lastIndex) statement += ","
                    addStatement(statement)
                }
            }
            .addStatement(")")
            .apply {
                if (needsParentRef) {
                    endControlFlow()
                }
                info.parameters.forEach { (name, type) ->
                    when {
                        type.isCollection && type.arguments.single().needsParentRef -> {
                            when (type.typeName) {
                                List::class.asClassName() -> addStatement("obj.$name = $name!!.map { it(obj) }")
                                Set::class.asClassName() -> addStatement("obj.$name = $name!!.map { it(obj) }.toSet()")
                                else -> error("unsupported collection $type")
                            }
                        }
                        type.needsParentRef -> {
                            addStatement("obj.$name = $name?.invoke(obj)")
                        }
                    }
                }
            }
            .addStatement("return obj")
            .build()
    }

    private fun enumSerializer(className: ClassName): FunSpec {
        return FunSpec.builder("value${className.simpleName}")
            .receiver(JsonWriter::class)
            .addParameter("obj", className.copy(nullable = true))
            .beginControlFlow("if (obj == null) {")
            .addStatement("nullValue()")
            .addStatement("return")
            .endControlFlow()
            .addStatement("value(obj.name)")
            .build()
    }

    private fun enumDeserializer(className: ClassName): FunSpec {
        val enumClassName = className.simpleName

        return FunSpec.builder("next$enumClassName")
            .receiver(JsonReader::class)
            .returns(className)
            .addStatement("val enumString = nextString()")
            .addStatement("return $enumClassName.values().firstOrNull { it.name.equals(enumString, ignoreCase = true) } ?: error(\"enumValue '${'$'}enumString' not found\")")
            .build()
    }

    private fun interfaceSerializer(info: KotlinSerializerInterfaceInfo): FunSpec {
        return FunSpec.builder("value${info.name.simpleName}")
            .receiver(JsonWriter::class)
            .addParameter("obj", info.name.copy(nullable = true))
            .beginControlFlow("if (obj == null) {")
            .addStatement("nullValue()")
            .addStatement("return")
            .endControlFlow()
            .addStatement("beginArray()")
            .beginControlFlow("when (obj) {")
            .apply {
                info.implementations.forEach {
                    beginControlFlow("is %T -> {", it)
                        .addStatement("value(%S)", it)
                        .addStatement("%M(obj)", it.getValueFunction())
                    endControlFlow()
                }
            }
            .addStatement("""else -> error("no serialisation configured for: ${'$'}obj")""")
            .endControlFlow()
            .addStatement("endArray()")
            .build()
    }

    private fun interfaceDeserializer(interfaceName: ClassName, serializerInfo: KotlinSerializerInfo): FunSpec {
        val interfaceInfo = serializerInfo.interfaces.first { it.name == interfaceName }

        val returnType = if (interfaceInfo.commonNeededParentRef != null) {
            LambdaTypeName.get(
                parameters = listOf(ParameterSpec("", interfaceInfo.commonNeededParentRef)),
                returnType = interfaceName
            )

        } else {
            interfaceName
        }

        return FunSpec.builder("next${interfaceName.simpleName}")
            .receiver(JsonReader::class)
            .returns(returnType)
            .addStatement("beginArray()")
            .addStatement("val type = nextString()")
            .beginControlFlow("val obj = when (type) {")
            .apply {
                interfaceInfo.implementations.forEach {
                    addStatement("%S -> %M()", it, it.getJsonNextFunction())
                }
            }
            .addStatement("""else -> error("unknown type ${'$'}type")""")
            .endControlFlow()
            .addStatement("endArray()")
            .addStatement("return obj")
            .build()
    }

    private fun ClassName.getJsonNextFunction(): MemberName {
        check(this.packageName != "kotlin") { this }
        return MemberName(packageName, "next$simpleName")
    }

    private fun ClassName.getValueFunction(): MemberName {
        check(this.packageName != "kotlin") { this }
        return MemberName(packageName, "value$simpleName")
    }
}
