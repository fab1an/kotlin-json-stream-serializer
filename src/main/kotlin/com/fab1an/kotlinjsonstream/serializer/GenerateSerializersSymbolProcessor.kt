package com.fab1an.kotlinjsonstream.serializer

import com.fab1an.kotlinjsonstream.serializer.annotations.Ser
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec

class GenerateSerializersSymbolProcessor(
    val kspCodeGenerator: com.google.devtools.ksp.processing.CodeGenerator,
    val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        /* parse all files */
        val serializationInfo = CodeParser().parse(
            resolver
                .getSymbolsWithAnnotation(Ser::class.qualifiedName!!)
                .map { it as KSClassDeclaration }
        )

        /* generate fileSpecs for constructors and interfaces */
        val files = mutableMapOf<ClassName, FileSpec>()
        serializationInfo.constructors.forEach {
            files += it.name to CodeGenerator().createConstructorSerializerFileSpec(it)
        }
        serializationInfo.interfaces.forEach {
            files += it.name to CodeGenerator().createInterfaceSerializerFileSpec(it)
        }

        /* create new files and assign to input */
        files.forEach { (className, fileSpec) ->
            val dependentFromKSFiles = serializationInfo.typeToKSFileMap.getValue(className)
            val aggregatingOutput = serializationInfo.aggregatingOutput.getValue(className)

            logger.info("generating $className (aggregating: $aggregatingOutput, dependentFromKSFiles: ${dependentFromKSFiles.size})")

            try {
                kspCodeGenerator.createNewFile(
                    Dependencies(aggregating = aggregatingOutput, *dependentFromKSFiles.toTypedArray()),
                    fileSpec.packageName,
                    fileSpec.name
                ).bufferedWriter().use {
                    fileSpec.writeTo(it)
                }

            } catch (ex: FileAlreadyExistsException) {
                logger.info("ignoring FileAlreadyExistsException: ${ex.message}")
            }
        }

        return emptyList()
    }

}
