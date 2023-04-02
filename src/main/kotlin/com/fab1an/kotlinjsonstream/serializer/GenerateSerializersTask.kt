package com.fab1an.kotlinjsonstream.serializer

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import javax.inject.Inject

@CacheableTask
internal open class GenerateSerializersTask @Inject constructor(
    @InputDirectory
    @SkipWhenEmpty
    @PathSensitive(value = PathSensitivity.NAME_ONLY)
    val inputDir: Path,

    @OutputDirectory
    val outputDir: Path
) : DefaultTask() {

    @TaskAction
    fun generate() {
        Files.walk(inputDir).use { pathStream ->
            val pathList = pathStream.collect(Collectors.toList())

            val info = CodeParser().parse(
                pathList
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .map {
                        Files.readString(it)
                    }
            )

            SerializationFileWriter(outputDir).write(info)
        }
    }
}
