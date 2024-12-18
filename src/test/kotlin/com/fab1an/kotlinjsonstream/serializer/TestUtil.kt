package com.fab1an.kotlinjsonstream.serializer

import com.fab1an.kotlinjsonstream.serializer.annotations.Ser
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.assertEquals

infix fun <T> T.shouldEqual(expected: T) {
    assertEquals(expected, this)
}

internal fun CodeParser.parseSource(source: SourceFile): KotlinSerializerInfo {
    return parseSource(listOf(source))
}

@OptIn(ExperimentalCompilerApi::class)
internal fun CodeParser.parseSource(sources: List<SourceFile>): KotlinSerializerInfo {
    var serializationInfo: KotlinSerializerInfo? = null

    KotlinCompilation().apply {
        this.sources = sources
        this.inheritClassPath = true
        symbolProcessorProviders = listOf(object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                return object : SymbolProcessor {
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        serializationInfo = CodeParser().parse(
                            resolver
                                .getSymbolsWithAnnotation(Ser::class.qualifiedName!!)
                                .map { it as KSClassDeclaration }
                        )
                        return emptyList()
                    }
                }
            }
        })
    }.compile()

    return serializationInfo!!
}
