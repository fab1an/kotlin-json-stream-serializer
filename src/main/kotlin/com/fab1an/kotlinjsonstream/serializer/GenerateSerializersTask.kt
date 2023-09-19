package com.fab1an.kotlinjsonstream.serializer

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
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
    @get:InputDirectory
    @get:SkipWhenEmpty
    @get:PathSensitive(value = PathSensitivity.NAME_ONLY)
    val inputDir: Path,

    @get:OutputDirectory
    val outputDir: Provider<Path>
) : DefaultTask() {

    @TaskAction
    fun generate() {
        Files.walk(inputDir).use { pathStream ->
            val pathList = pathStream.collect(Collectors.toList())

            val info = CodeParser().parse(
                pathList
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .map {
                        it to Files.readString(it)
                    }
                    .filter {
                        if ("::class" in it.second) {
                            println("[kotlin-serializers] skipping ${inputDir.relativize(it.first)} because it contains ::class, which is unsupported")
                            false
                        } else
                            true
                    }
                    .map {
                        it.second
                    }

            )

            SerializationFileWriter(outputDir.get()).write(info)
        }
    }
}
