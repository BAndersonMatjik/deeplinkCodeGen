package com.bmatjik.deeplinkcodegen.processor

import com.bmatjik.deeplinkcodegen.annotations.DeepLinkDestination
import com.bmatjik.deeplinkcodegen.annotations.Destination
import com.bmatjik.deeplinkcodegen.processor.Meta.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.PrintWriter
import java.util.function.Consumer
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.JavaFileObject


@AutoService(Processor::class) // For registering the service
@SupportedOptions(Meta.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class DeeplinkProcessor : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        val setAnnotation = LinkedHashSet<String>()
        setAnnotation.add(Destination::class.java.canonicalName)
        return setAnnotation
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Start DeepLinkProcessor")
        val annotations = roundEnv?.getElementsAnnotatedWith(Destination::class.java)
        annotations?.forEach { classElement ->

            if (classElement.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR, "Can only apply to Class, element : $classElement"
                )
                return false
            }
            processAnnotation(classElement)

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

    private fun cleanupName(rawName: String, removeGetterSetter: Boolean): String {
        return rawName.replace("\$annotations", "").let {
            if (removeGetterSetter) {
                it.removePrefix("get")
                    .removePrefix("set")
            } else {
                it
            }
        }
    }

    private fun processAnnotation(element: Element) {


    }

}

object GenerateDeeplink {
    fun execute(fileName: String, packageName: String, deeplinkName: String, originClassName: ClassName) {
        val className = ClassName(packageName, fileName)
        val file = FileSpec.builder(packageName, fileName).addType(
            TypeSpec.classBuilder(fileName + "DeepLinkProcessor")
                .primaryConstructor(
                    FunSpec.constructorBuilder().addParameter(
                        ParameterSpec.builder("context", getContext()).addAnnotation(
                            getDaggerHiltQualifierPackageName()
                        ).build()
                    )
                        .addAnnotation(getJavaxInject()).build()
                )
                .addSuperinterface(DeepLinkDestination::class)
                .addFunctions(
                    listOf(
                        FunSpec.builder("matches").addModifiers(KModifier.OVERRIDE)
                            .addParameter("deeplink", String::class).returns(Boolean::class)
                            .addStatement("val uri = Uri.parse(deeplink)")
                            .addStatement(
                                "val filtered = uri.pathSegments.filter { it.equals(%S,ignoreCase = true) }",
                                deeplinkName
                            )
                            .beginControlFlow("if(filtered.isNotEmpty())").addStatement("return true").endControlFlow()
                            .addStatement("return false").build(),
                        FunSpec.builder("execute").addParameter("deeplink", String::class)
                            .addStatement("val uri = Uri.parse(deeplink)").beginControlFlow(
                                " context.startActivity(Intent(context, %T::class.java).apply", originClassName
                            ).beginControlFlow("uri.queryParameterNames.forEach")
                            .beginControlFlow("if (it.isNotBlank())")
                            .beginControlFlow("it?.let")
                            .addStatement(
                                ("putExtra(it, uri.getQueryParameter(it))")
                            ).endControlFlow().endControlFlow().addStatement("flags = Intent.FLAG_ACTIVITY_NEW_TASK")
                            .endControlFlow().build()
                    )
                )
                .build()
                .apply {
                    println("${this}")
                }
        ).addImport("android.net", "Uri")
    }

    private const val ANDROID_CONTENT_PACKAGE_NAME = "android.content"
    private const val JAVAX_INJECT_PACKAGE_NAME = "javax.inject"
    private const val DAGGER_HILT_QUALIFIER_PACKAGE_NAME = "dagger.hilt.android.qualifiers"
    private fun getContext(): TypeName {
        val context = ClassName(ANDROID_CONTENT_PACKAGE_NAME, "Context")
        return WildcardTypeName.consumerOf(context)
    }

    private fun getJavaxInject(): ClassName {
        val context = ClassName(JAVAX_INJECT_PACKAGE_NAME, "Inject")
        return context
    }

    private fun getDaggerHiltQualifierPackageName(): ClassName {
        return ClassName(DAGGER_HILT_QUALIFIER_PACKAGE_NAME, "ApplicationContext")
    }


}

