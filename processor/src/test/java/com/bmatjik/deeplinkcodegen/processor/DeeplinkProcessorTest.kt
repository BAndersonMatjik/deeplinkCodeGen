package com.bmatjik.deeplinkcodegen.processor

import com.bmatjik.deeplinkcodegen.annotations.Destination
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Test

class DeeplinkProcessorTest {
    @Test
    fun `test @Destination Annotation Processor`() {
        val kotlinSource = SourceFile.kotlin(
            "KClass.kt", """
        package com.bmatjik.deeplinkcodegen.processor
        @com.bmatjik.deeplinkcodegen.annotations.Destination
        class KClass {
            fun foo() {
                // Classes from the test environment are visible to the compiled sources
            }
        }
    """
        )
        val result = KotlinCompilation().apply {
            sources = listOf(kotlinSource)

//            // pass your own instance of an annotation processor
            annotationProcessors = listOf(DeeplinkProcessor())

            // pass your own instance of a compiler plugin
//            compilerPlugins = listOf(MyComponentRegistrar())
//            commandLineProcessors = listOf(Deeplin())

            inheritClassPath = true
            messageOutputStream = System.out // see diagnostics in real time
        }.compile()


    }
}