plugins {
    kotlin("jvm").version("2.4.21")
    id("publishing-conventions")
}

group = "com.fab1an"
version = "2.0.5-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.12")
    implementation("com.fab1an:kotlin-json-stream-serializer-annotations:2.0.1")
    implementation("com.squareup:kotlinpoet:2.4.0")
    implementation("com.fab1an:kotlin-json-stream:1.2.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("dev.zacsweers.kctfork:ksp:0.14.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
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
