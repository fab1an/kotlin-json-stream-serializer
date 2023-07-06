package com.fab1an.kotlinjsonstream.serializer

import com.squareup.kotlinpoet.ClassName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

class SerializationFileWriterTest {

    @Test
    fun cleanOldFiles() {

        val tempDir = Files.createTempDirectory("temp")

        /* create files */
        var tempFile = tempDir.resolve(Paths.get("com", "fab1an", "kotlinjsonserializer", "test", "ASerialization.kt"))
        Files.createDirectories(tempFile.parent)
        Files.createFile(tempFile)
        tempFile = tempDir.resolve(Paths.get("com", "fab1an", "kotlinjsonserializer", "test", "BSerialization.kt"))
        Files.createDirectories(tempFile.parent)
        Files.createFile(tempFile)
        tempFile = tempDir.resolve(Paths.get("com", "fab1an", "kotlinjsonserializer", "CSerialization.kt"))
        Files.createDirectories(tempFile.parent)
        Files.createFile(tempFile)

        Files.walk(tempDir).use { stream ->
            val pathList = stream.collect(Collectors.toList())
                .filter { Files.isRegularFile(it) }
                .map { tempDir.relativize(it) }
                .toSet()

            pathList shouldEqual setOf(
                Paths.get("com/fab1an/kotlinjsonserializer/CSerialization.kt"),
                Paths.get("com/fab1an/kotlinjsonserializer/test/ASerialization.kt"),
                Paths.get("com/fab1an/kotlinjsonserializer/test/BSerialization.kt")
            )
        }

        val handler = SerializationFileWriter(tempDir)
        handler.pruneFiles(
            activeClasses = listOf(
                ClassName("com.fab1an.kotlinjsonserializer.test", "A"),
                ClassName("com.fab1an.kotlinjsonserializer", "C")
            )
        )

        Files.walk(tempDir).use { stream ->
            val pathList = stream.collect(Collectors.toList())
                .filter { Files.isRegularFile(it) }
                .map { tempDir.relativize(it) }
                .toSet()

            pathList shouldEqual setOf(
                Paths.get("com/fab1an/kotlinjsonserializer/test/ASerialization.kt"),
                Paths.get("com/fab1an/kotlinjsonserializer/CSerialization.kt")
            )
        }
    }

    @Test
    fun dataInName() {

        val tempDir = Files.createTempDirectory("temp")

        /* create files */
        var tempFile = tempDir.resolve(Paths.get("com", "fab1an", "kotlinjsonserializer", "data", "ASerialization.kt"))
        Files.createDirectories(tempFile.parent)
        Files.createFile(tempFile)
        tempFile = tempDir.resolve(Paths.get("com", "fab1an", "kotlinjsonserializer", "CSerialization.kt"))
        Files.createDirectories(tempFile.parent)
        Files.createFile(tempFile)

        Files.walk(tempDir).use { stream ->
            val pathList = stream.collect(Collectors.toList())
                .filter { Files.isRegularFile(it) }
                .map { tempDir.relativize(it) }
                .toSet()

            pathList shouldEqual setOf(
                Paths.get("com/fab1an/kotlinjsonserializer/CSerialization.kt"),
                Paths.get("com/fab1an/kotlinjsonserializer/data/ASerialization.kt")
            )
        }

        val handler = SerializationFileWriter(tempDir)
        handler.pruneFiles(
            activeClasses = listOf(
                ClassName("com.fab1an.kotlinjsonserializer.data", "A"),
                ClassName("com.fab1an.kotlinjsonserializer", "C")
            )
        )

        Files.walk(tempDir).use { stream ->
            val pathList = stream.collect(Collectors.toList())
                .filter { Files.isRegularFile(it) }
                .map { tempDir.relativize(it) }
                .toSet()

            pathList shouldEqual setOf(
                Paths.get("com/fab1an/kotlinjsonserializer/CSerialization.kt"),
                Paths.get("com/fab1an/kotlinjsonserializer/data/ASerialization.kt")
            )
        }
    }
}
