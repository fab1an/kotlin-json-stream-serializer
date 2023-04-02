package com.fab1an.kotlinjsonstream.serializer

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class ApplyPluginTest {

    @TempDir
    @JvmField
    var testProjectDir: Path? = null

    private lateinit var settingsFile: Path
    private lateinit var buildFile: Path

    @BeforeEach
    fun setup() {
        settingsFile = testProjectDir!!.resolve("settings.gradle")
        buildFile = testProjectDir!!.resolve("build.gradle")
    }

    @Test
    fun testHelloWorldTask() {
        settingsFile.writeText("rootProject.name = 'hello-world'")
        buildFile.writeText("""
            plugins {
                id "org.jetbrains.kotlin.jvm" version "1.8.10"
                id "com.fab1an.kotlin-json-stream-serializer" version "1.0.0-SNAPSHOT"
            }

            repositories {
                mavenCentral()
            }

            task helloWorld { 
                doLast {        
                    println 'Hello world!'    
                }
            }
            """.trimIndent()
        )

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testProjectDir!!.toFile())
            .withArguments("helloWorld")
            .withPluginClasspath()
            .build()

        result.output.contains("Hello world!") shouldEqual true
        result.task(":helloWorld")?.outcome shouldEqual TaskOutcome.SUCCESS
    }
}
