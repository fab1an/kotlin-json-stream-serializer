package com.fab1an.kotlinjsonstream.serializer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.nio.file.Paths

class KotlinJsonStreamSerializerGradlePlugin : Plugin<Project> {

    private val kotlinJsonStreamDependency = "com.fab1an:kotlin-json-stream:1.1.1-SNAPSHOT"
    private val kotlinStreamSerializerAnnotationDependency =
        "com.fab1an:kotlin-json-stream-serializer-annotations:1.0.0-SNAPSHOT"

    override fun apply(project: Project) {

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") { plugin ->
            println("[kotlin-serializers] configuring plugin ${plugin.name}")

            project.kotlinExtension.sourceSets.configureEach { sourceSet ->
                println("[kotlin-serializers] configuring sourceSet ${sourceSet.name}")
                if (sourceSet.name == "commonMain") {
                    sourceSet.dependencies {
                        implementation(kotlinJsonStreamDependency)
                        implementation(kotlinStreamSerializerAnnotationDependency)
                    }
                }
                configureSourceSet(project, sourceSet)
            }
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") { plugin ->
            println("[kotlin-serializers] configuring plugin ${plugin.name}")

            project.kotlinExtension.sourceSets.configureEach { sourceSet ->
                println("[kotlin-serializers] configuring sourceSet ${sourceSet.name}")
                if (sourceSet.name == "main") {
                    sourceSet.dependencies {
                        implementation(kotlinJsonStreamDependency)
                        implementation(kotlinStreamSerializerAnnotationDependency)
                    }
                }
                configureSourceSet(project, sourceSet)
            }
        }
    }

    private fun configureSourceSet(project: Project, sourceSet: KotlinSourceSet) {
        val inputSrcDir = Paths.get("${project.rootDir}/src/${sourceSet.name}/kotlin")
        val outputSrcDir = Paths.get("${project.buildDir}/generated/source/kotlin-serializers/${sourceSet.name}/kotlin")

        val taskName = when {
            sourceSet.name == "main" -> {
                "generateSerializers"
            }

            sourceSet.name == "test" -> {
                "generateTestSerializers"
            }

            sourceSet.name.endsWith("Main") -> {
                "generateSerializers${sourceSet.name.substringBeforeLast("Main").capitalizeAscii()}"
            }

            else -> {
                "generateTestSerializers${sourceSet.name.substringBeforeLast("Test").capitalizeAscii()}"
            }
        }

        project.tasks.register(taskName, GenerateSerializersTask::class.java, inputSrcDir, outputSrcDir)
        sourceSet.kotlin.srcDir(project.tasks.named(taskName).map { it.outputs.files })
    }
}

