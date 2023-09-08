package com.fab1an.kotlinjsonstream.serializer

import com.fab1an.kotlinjsonstream.serializer.KotlinSerializerParameter.KotlinSerializerCollectionParameter
import com.fab1an.kotlinjsonstream.serializer.KotlinSerializerParameter.KotlinSerializerStandardParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import kastree.ast.Node
import kastree.ast.psi.Parser

internal class CodeParser {

    fun parse(stringSource: String): KotlinSerializerInfo {
        return parse(listOf(stringSource))
    }

    fun parse(sources: List<String>): KotlinSerializerInfo {
        val constructors = mutableListOf<KotlinSerializerConstructorInfo>()
        val interfaces = mutableListOf<KotlinSerializerInterfaceInfo>()
        val kotlinSerInterfaceImplementations = mutableMapOf<ClassName, MutableList<ClassName>>()

        sources.forEach { stringSource ->
            val fileNode = Parser.parseFile(stringSource)

            /* get packageName */
            val packageName = fileNode.pkg?.names?.joinToString(".") { it } ?: error("no package-name")

            /* get imports */
            val imports = getImports(fileNode.imports).toMutableMap()

            /* add names in file to imports */
            fileNode.decls
                .mapNotNull { it as? Node.Decl.Structured }
                .filter {
                    it.form == Node.Decl.Structured.Form.CLASS
                }
                .map {
                    ClassName(packageName, it.name)
                }
                .forEach {
                    imports[it.simpleName] = it
                }

            /* get elements annotated with Ser */
            val annotatedWithSer = fileNode.decls
                .mapNotNull { it as? Node.Decl.Structured }
                .filter { declaration ->
                    declaration.mods
                        .mapNotNull { it as? Node.Modifier.AnnotationSet }
                        .any { annotationSet ->
                            annotationSet.anns.any { it.names.singleOrNull() == "Ser" }
                        }
                }

            /* interfaces */
            annotatedWithSer
                .filter { it.form == Node.Decl.Structured.Form.INTERFACE }
                .forEach {
                    val interfaceName = ClassName(packageName, it.name)

                    if (kotlinSerInterfaceImplementations[interfaceName] == null) {
                        kotlinSerInterfaceImplementations[interfaceName] = mutableListOf()
                    }

                    interfaces += KotlinSerializerInterfaceInfo(
                        name = interfaceName,
                        implementations = kotlinSerInterfaceImplementations[interfaceName]!!,
                        commonNeededParentRef = null
                    )
                }

            /* enums */
            annotatedWithSer
                .filter { it.form == Node.Decl.Structured.Form.ENUM_CLASS }
                .forEach { declaration ->
                    constructors += KotlinSerializerConstructorInfo(
                        name = ClassName(packageName, declaration.name),
                        parameters = emptyMap(),
                        isEnum = true
                    )
                }

            /* constructors */
            annotatedWithSer
                .filter { it.form == Node.Decl.Structured.Form.CLASS }
                .forEach { declaration ->
                    val className = ClassName(packageName, declaration.name)
                    if (declaration.parents.isNotEmpty()) {
                        declaration.parents
                            .mapNotNull { it as? Node.Decl.Structured.Parent.Type }
                            .forEach {
                                val name = resolveTypeName(packageName, imports, it.type)
                                if (kotlinSerInterfaceImplementations[name] == null) {
                                    kotlinSerInterfaceImplementations[name] = mutableListOf()
                                }
                                kotlinSerInterfaceImplementations[name]?.add(className)
                            }
                    }

                    val listOfConstructorParamLists: MutableList<List<Node.Decl.Func.Param>> = declaration.members
                        .mapNotNull { it as? Node.Decl.Constructor }
                        .map { it.params }
                        .toMutableList()

                    declaration.primaryConstructor?.let {
                        listOfConstructorParamLists += it.params
                    }

                    if (listOfConstructorParamLists.isEmpty()) {
                        listOfConstructorParamLists.add(emptyList())
                    }

                    val selectedConstructorParamList = listOfConstructorParamLists
                        .maxBy { it.size }

                    val kotlinSerTypes = mutableMapOf<String, KotlinSerializerParameter>()
                    selectedConstructorParamList.forEach { param ->
                        val isParentRef = param.anns.any { annotationSet ->
                            annotationSet.anns.any { it.names.first() == "ParentRef" }
                        }
                        kotlinSerTypes += param.name to parseType(
                            packageName,
                            imports,
                            param.type?.ref ?: error("param $param has no type"),
                            isParentRef = isParentRef
                        )
                    }

                    constructors += KotlinSerializerConstructorInfo(
                        name = className,
                        parameters = kotlinSerTypes,
                        isEnum = false
                    )
                }

            /* find types needing parent-ref */
            val typesNeedingParentRef = mutableListOf<ClassName>()
            constructors.forEach { constructor ->
                if (constructor.parameters
                        .values
                        .filterIsInstance<KotlinSerializerStandardParameter>()
                        .any { it.isParentRef }
                ) {
                    typesNeedingParentRef += constructor.name
                }
            }

            /* add it to kotlinSer */
            constructors.forEach { constructor ->
                constructor.parameters.values.forEach { typeInfo ->
                    setNeedsParentRef(typesNeedingParentRef, typeInfo)
                }
            }
        }

        interfaces.forEachIndexed { index, interfaceInfo ->
            val neededParentRef: ClassName? =
                constructors
                    .filter { it.name in interfaceInfo.implementations }
                    .flatMap { it.parameters.values }
                    .filterIsInstance<KotlinSerializerStandardParameter>()
                    .firstOrNull { it.isParentRef }
                    ?.typeName

            interfaces[index] = interfaceInfo.copy(commonNeededParentRef = neededParentRef)
        }
        return KotlinSerializerInfo(constructors, interfaces)
    }

    private fun setNeedsParentRef(typesNeedingParentRef: List<ClassName>, type: KotlinSerializerParameter) {
        when (type) {
            is KotlinSerializerStandardParameter -> {
                if (type.typeName in typesNeedingParentRef) {
                    type.needsParentRef = true
                }
            }

            is KotlinSerializerCollectionParameter -> {
                setNeedsParentRef(typesNeedingParentRef, type.argument)
            }
        }
    }

    private fun resolveTypeName(
        packageName: String,
        imports: Map<String, ClassName>,
        typeRefNode: Node.TypeRef
    ): ClassName {
        when (typeRefNode) {
            is Node.TypeRef.Simple -> {
                check(typeRefNode.pieces.size == 1) { "$typeRefNode has many pieces" }
                val rawIdentifier = typeRefNode.pieces.single().name
                return when {
                    "." in rawIdentifier -> ClassName(
                        rawIdentifier.substringBeforeLast("."),
                        rawIdentifier.substringAfterLast(".")
                    )

                    rawIdentifier in imports -> imports[rawIdentifier]!!
                    rawIdentifier == "List" -> List::class.asClassName()
                    rawIdentifier == "Set" -> Set::class.asClassName()
                    rawIdentifier == "Int" -> Int::class.asClassName()
                    rawIdentifier == "Boolean" -> Boolean::class.asClassName()
                    rawIdentifier == "String" -> String::class.asClassName()
                    rawIdentifier == "Double" -> Double::class.asClassName()
                    else -> ClassName(packageName, rawIdentifier)
                }
            }

            else -> error(typeRefNode)
        }
    }

    private fun parseType(
        packageName: String,
        imports: Map<String, ClassName>,
        typeRefNode: Node.TypeRef,
        isParentRef: Boolean
    ): KotlinSerializerParameter {
        when (typeRefNode) {
            is Node.TypeRef.Simple -> {
                check(typeRefNode.pieces.size == 1) { "$typeRefNode has more than one piece" }
                val typeRefNodePiece = typeRefNode.pieces.single()

                when (val parameterType = resolveTypeName(packageName, imports, typeRefNode)) {
                    List::class.asClassName(), Set::class.asClassName() -> {
                        check(!isParentRef) { "$typeRefNode collectionType cannot be parent-reference" }
                        return KotlinSerializerCollectionParameter(
                            typeName = parameterType,
                            argument = parseType(
                                packageName,
                                imports,
                                typeRefNodePiece.typeParams.single()!!.ref,
                                false
                            )
                        )
                    }

                    else -> {
                        return KotlinSerializerStandardParameter(
                            isMarkedNullable = false,
                            typeName = parameterType,
                            isParentRef = isParentRef,
                            needsParentRef = false // set later
                        )
                    }
                }
            }

            is Node.TypeRef.Nullable -> {
                when (val kotlinSerializerParameter = parseType(packageName, imports, typeRefNode.type, isParentRef)) {
                    is KotlinSerializerStandardParameter -> {
                        return kotlinSerializerParameter.copy(isMarkedNullable = true)
                    }

                    is KotlinSerializerCollectionParameter -> {
                        error("$kotlinSerializerParameter is a collection therefore it cannot be nullable")
                    }
                }
            }

            else -> error(typeRefNode)
        }
    }

    private fun getImports(imports: List<Node.Import>): Map<String, ClassName> {
        return imports
            .filterNot { it.wildcard }
            .associate { import ->
                val joined = import.names.joinToString(".")
                val className = joined.substringAfterLast(".")
                val packageName = joined.substringBeforeLast(".")

                className to ClassName(packageName, className)
            }
    }
}

