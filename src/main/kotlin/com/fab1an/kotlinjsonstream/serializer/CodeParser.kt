package com.fab1an.kotlinjsonstream.serializer

import com.fab1an.kotlinjsonstream.serializer.KotlinSerializerParameter.KotlinSerializerCollectionParameter
import com.fab1an.kotlinjsonstream.serializer.KotlinSerializerParameter.KotlinSerializerStandardParameter
import com.fab1an.kotlinjsonstream.serializer.annotations.ParentRef
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import kotlin.collections.single
import kotlin.sequences.filter

internal class CodeParser {

    private val KSDeclaration.className: ClassName
        get() = ClassName(packageName.asString(), simpleName.asString())

    fun parse(annotatedWithSer: Sequence<KSClassDeclaration>): KotlinSerializerInfo {
        val constructors = mutableListOf<KotlinSerializerConstructorInfo>()
        val interfaces = mutableListOf<KotlinSerializerInterfaceInfo>()
        val kotlinSerInterfaceImplementations = mutableMapOf<ClassName, MutableList<ClassName>>()

        val typeToKSFileMap = mutableMapOf<ClassName, MutableList<KSFile>>()
        val aggregatingOutput = mutableMapOf<ClassName, Boolean>()

        /* interfaces */
        annotatedWithSer
            .filter { it.classKind == ClassKind.INTERFACE }
            .forEach { interfaceDeclaration ->
                val interfaceClassName = interfaceDeclaration.className

                interfaces += KotlinSerializerInterfaceInfo(
                    name = interfaceClassName,
                    /* reference to other list */
                    implementations = kotlinSerInterfaceImplementations.getOrPut(interfaceClassName, ::mutableListOf),
                    commonNeededParentRef = null
                )

                typeToKSFileMap.getOrPut(interfaceClassName, ::mutableListOf).add(interfaceDeclaration.containingFile!!)
                aggregatingOutput[interfaceClassName] = true
            }

        /* enums */
        annotatedWithSer
            .filter { it.classKind == ClassKind.ENUM_CLASS }
            .forEach { enumDeclaration ->
                val enumClassName = enumDeclaration.className

                constructors += KotlinSerializerConstructorInfo(
                    name = enumClassName,
                    parameters = emptyMap(),
                    isEnum = true
                )

                typeToKSFileMap.getOrPut(enumClassName, ::mutableListOf).add(enumDeclaration.containingFile!!)
                aggregatingOutput[enumClassName] = false
            }

        /* constructors */
        annotatedWithSer
            .filter { it.classKind == ClassKind.CLASS }
            .forEach { classDeclaration ->
                val className = classDeclaration.className

                classDeclaration.superTypes
                    .forEach { superType ->
                        val superTypeName = superType.resolve().declaration.className

                        kotlinSerInterfaceImplementations.getOrPut(superTypeName, ::mutableListOf)
                            .add(className)
                    }

                val listOfConstructorParamLists = classDeclaration.getConstructors()
                    .map { it.parameters }
                    .toMutableList()

                classDeclaration.primaryConstructor?.let {
                    listOfConstructorParamLists += it.parameters
                }

                if (listOfConstructorParamLists.isEmpty()) {
                    listOfConstructorParamLists.add(emptyList())
                }

                val selectedConstructorParamList = listOfConstructorParamLists
                    .maxBy { it.size }

                val kotlinSerTypes = mutableMapOf<String, KotlinSerializerParameter>()
                selectedConstructorParamList.forEach { param ->
                    val isParentRef =
                        param.annotations.any { annotation ->
                            annotation.annotationType.resolve().declaration.qualifiedName?.asString() ==
                                    ParentRef::class.qualifiedName
                        }

                    kotlinSerTypes += param.name!!.asString() to parseType(
                        param.type,
                        isParentRef = isParentRef
                    )
                }

                constructors += KotlinSerializerConstructorInfo(
                    name = className,
                    parameters = kotlinSerTypes,
                    isEnum = false
                )

                typeToKSFileMap.getOrPut(className, ::mutableListOf)
                    .add(classDeclaration.containingFile!!)
                aggregatingOutput[className] = false
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

        interfaces.forEach { interfaceInfo ->
            interfaceInfo.implementations.forEach { implementation ->
                typeToKSFileMap.getOrPut(interfaceInfo.name, ::mutableListOf)
                    .add(typeToKSFileMap.getValue(implementation).single())
            }
        }

        return KotlinSerializerInfo(
            constructors,
            interfaces,
            typeToKSFileMap,
            aggregatingOutput
        )
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

    private fun parseType(
        ksTypeRef: KSTypeReference,
        isParentRef: Boolean
    ): KotlinSerializerParameter {
        val parameterTypeName = ksTypeRef.resolve().declaration.className

        when (parameterTypeName) {
            List::class.asClassName(), Set::class.asClassName() -> {
                check(!isParentRef) { "$ksTypeRef collectionType cannot be parent-reference" }
                check(!ksTypeRef.resolve().isMarkedNullable) { "$ksTypeRef collectionType cannot be nullable" }
                return KotlinSerializerCollectionParameter(
                    typeName = parameterTypeName,
                    argument = parseType(
                        ksTypeRef.resolve().arguments.single().type!!,
                        false
                    )
                )
            }

            else -> {
                return KotlinSerializerStandardParameter(
                    isMarkedNullable = ksTypeRef.resolve().isMarkedNullable,
                    typeName = parameterTypeName,
                    isParentRef = isParentRef,
                    needsParentRef = false // set later
                )
            }
        }
    }
}

