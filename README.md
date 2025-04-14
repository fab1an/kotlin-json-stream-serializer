# Kotlin JSON stream serializer - Compile-time generated Kotlin Multiplatform JSON stream serialization

[![maven central version](https://img.shields.io/maven-central/v/com.fab1an/kotlin-json-stream-serializer)](https://mvnrepository.com/artifact/com.fab1an/kotlin-json-stream-serializer)
[![semver](https://img.shields.io/:semver-%E2%9C%93-brightgreen.svg)](http://semver.org/)
[![license](https://img.shields.io/github/license/fab1an/kotlin-json-stream-serializer)](https://github.com/fab1an/kotlin-json-stream-serializer/blob/master/LICENSE)
[![build status](https://github.com/fab1an/kotlin-json-stream-serializer/actions/workflows/build-master.yml/badge.svg)](https://github.com/fab1an/kotlin-json-stream-serializer/actions/workflows/build-master.yml)
[![OpenSSF ScoreCard](https://img.shields.io/ossf-scorecard/github.com/fab1an/kotlin-json-stream-serializer)](https://scorecard.dev/)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/10362/badge)](https://www.bestpractices.dev/projects/10362)


## Introduction

Kotlin JSON stream serializer is a [KSP](https://github.com/google/ksp) plugin, that allows you to annotate certain classes and have JSON serialization/deserialization functions generated for them at compile-time.

**Sample**: For a sample project go to [kotlin-json-stream-serializer-sample](https://github.com/fab1an/kotlin-json-stream-serializer-sample).

## Setup

### Setup for JVM

````gradle
plugins {
    kotlin("jvm").version("2.1.20")
    id("com.google.devtools.ksp") version("2.1.20-1.0.31")
}

repositories {
    mavenCentral()
}

dependencies {
    ksp("com.fab1an:kotlin-json-stream-serializer:2.0.4")
    implementation("com.fab1an:kotlin-json-stream-serializer-annotations:2.0.1")
    implementation("com.fab1an:kotlin-json-stream:1.2.3")
}
````
### Setup for Kotlin Multiplatform

````gradle
plugins {
    kotlin("multiplatform").version("2.1.10")
    id("com.google.devtools.ksp").version("2.1.10-1.0.30")
}

repositories {
    mavenCentral()
}

kotlin {
    macosX64()
    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.fab1an:kotlin-json-stream-serializer-annotations:2.0.0")
                implementation("com.fab1an:kotlin-json-stream:1.2.3")
            }
            kotlin {
                srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "com.fab1an:kotlin-json-stream-serializer:2.0.3")
}

tasks.getByName("compileKotlinJvm").dependsOn("kspCommonMainKotlinMetadata")
tasks.getByName("compileKotlinMacosX64").dependsOn("kspCommonMainKotlinMetadata")
````

## Usage

### Basic Serialization

Use the annotations `@Ser` on a class to have serializers generated. The serialization will always pick the constructor with the largest number of arguments and work on it's parameters:

* For deserialization, the detected parameters will be read from the JSON-stream and passed into the constructor to build the object.
* For serialization, the detected parameters will be read from the object, it is necessary to have public field-access for them.

Annotating a class `MyClass` with `@Ser` will create two extension functions:

* `JsonReader.nextMyClass(): MyClass` for reading the object from the stream
* `JsonWriter.valueMyClass(myClass: MyClass)` for writing the object to the stream

### Custom types

To support custom types you need to create two extension functions in the same package as the type:

* `JsonReader.nextMyType(): MyType` for reading the type from the stream
* `JsonWriter.valueMyType(myType: MyType)` for writing the type to the stream


### Polymorphism

Polymorphism is supported automatically when you annotate an interface using `@Ser`. The plugin will find all implementations and write their type and data into an array during serialization and use it during deserialization.

### Circular References

To serialize and deserialize circular structures you need to

* Annotate both parent and child objects using `@Ser`.
* Have the parent object passed into the child as a constructor parameter and annotate it using `@ParentRef`.
* Have the child object stored in the parent object in a variable `List` or a nullable parameter.

### Caveats

* ENUMs and field names are *deserialized* case-insensitively, they are serialized with their original case.
