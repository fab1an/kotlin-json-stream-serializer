plugins {
    kotlin("jvm").version("2.1.10")
    id("publishing-conventions")
}

group = "com.fab1an"
version = "2.0.5-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.21-2.0.1")
    implementation("com.fab1an:kotlin-json-stream-serializer-annotations:2.0.1")
    implementation("com.squareup:kotlinpoet:2.1.0")
    implementation("com.fab1an:kotlin-json-stream:1.2.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}
