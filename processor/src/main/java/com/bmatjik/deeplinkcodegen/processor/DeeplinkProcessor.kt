package com.bmatjik.deeplinkcodegen.processor

import com.bmatjik.deeplinkcodegen.annotations.Destination
import com.bmatjik.deeplinkcodegen.processor.Meta.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
@SupportedOptions(Meta.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class DeeplinkProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv?.getElementsAnnotatedWith(Destination::class.java)?.forEach { classElement ->
            if (classElement.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR, "Can only apply to Class, element : $classElement"
                )
                return false
            }
        }
        val generatedSourcesRoot: String = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
        if (generatedSourcesRoot.isEmpty()) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can't find the target directory for generated Kotlin files."
            )
            return false
        }
        return true
    }
}