package com.fab1an.kotlinjsonstream.serializer

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class GenerateSerializersSymbolProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return GenerateSerializersSymbolProcessor(environment.codeGenerator, environment.logger)
    }
}
