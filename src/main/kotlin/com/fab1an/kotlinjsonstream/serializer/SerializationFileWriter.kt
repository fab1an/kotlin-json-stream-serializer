package com.fab1an.kotlinjsonstream.serializer

import com.squareup.kotlinpoet.ClassName
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

internal class SerializationFileWriter(private val generatedSourceDir: Path)  {

    fun write(info: KotlinSerializerInfo) {
        Files.createDirectories(generatedSourceDir)

        val fileSpecs = CodeGenerator().createSerializationFileSpecs(info)

        /* create new files in correct package */
        fileSpecs.forEach { file ->
            file.writeTo(generatedSourceDir)
        }

        /* remove old files */
        pruneFiles(info.constructors.map { it.name } + info.interfaces.map { it.name })
    }

    fun pruneFiles(activeClasses: List<ClassName>) {
        val activeFileNames = activeClasses.map { it.canonicalName + "Serialization.kt" }

        /* find all classes in directory and delete unnecessary */
        Files.walk(generatedSourceDir).use { stream ->
            val pathList = stream.collect(Collectors.toList())

            pathList
                .filter { Files.isRegularFile(it) }
                .filter {
                    val relPath = generatedSourceDir.relativize(it)
                    val simpleName = relPath.joinToString(".")
                    simpleName !in activeFileNames

                }.forEach {
                    Files.delete(it)
                }
        }
    }
}
