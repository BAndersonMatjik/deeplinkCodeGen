package com.bmatjik.deeplinkcodegen.processor

import com.bmatjik.deeplinkcodegen.annotations.DeepLinkDestination
import com.bmatjik.deeplinkcodegen.annotations.Destination
import com.bmatjik.deeplinkcodegen.processor.Meta.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


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
        val className = element.simpleName.toString()
        val pack = processingEnv.elementUtils.getPackageOf(element).toString()
        val fileName = "${className}DeepLinkDestination"

        val fileBuilder = FileSpec.builder(pack, fileName)
        val classBuilder = TypeSpec.classBuilder(fileName)

        val file = GenerateDeeplink.execute(fileName,pack,className,className)
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir))
    }

}

object GenerateDeeplink {
    fun execute(fileName: String, packageName: String, deeplinkName: String, originClassName: String): FileSpec {
        return FileSpec.builder(packageName, fileName).addType(
            TypeSpec.classBuilder(fileName)
                .primaryConstructor(
                    FunSpec.constructorBuilder().addParameter(
                        ParameterSpec.builder("context", getContext()).addAnnotation(
                            getDaggerHiltQualifierPackageName()
                        ).build()
                    )
                        .addAnnotation(getJavaxInject()).build()
                )
                .addSuperinterface(getDeepLinkDestination())
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
                        FunSpec.builder("execute").addModifiers(KModifier.OVERRIDE).addParameter("deeplink", String::class)
                            .addStatement("val uri = Uri.parse(deeplink)").beginControlFlow(
                                " context.startActivity(Intent(context, %L::class.java)).apply", originClassName
                            ).beginControlFlow("uri.queryParameterNames.forEach")
                            .beginControlFlow("if (it.isNotBlank())")
                            .beginControlFlow("it?.let")
                            .addStatement(
                                ("putExtra(it, uri.getQueryParameter(it))")
                            ).endControlFlow().endControlFlow()
                            .endControlFlow().addStatement(
                                "flags = %M.FLAG_ACTIVITY_NEW_TASK",
                                getContentIntentMemberName()
                            ).endControlFlow().build()
                    )
                )
                .build()
        ).addImport("android.net", "Uri").addImport(packageName,originClassName).build()
    }

    private const val ANDROID_CONTENT_PACKAGE_NAME = "android.content"
    private const val JAVAX_INJECT_PACKAGE_NAME = "javax.inject"
    private const val DAGGER_HILT_QUALIFIER_PACKAGE_NAME = "dagger.hilt.android.qualifiers"
    private const val DEEPLINK_INTERFACE ="id.anteraja.guardian.provider.navigation.deeplink"
    private fun getContext(): ClassName {
        val context = ClassName(ANDROID_CONTENT_PACKAGE_NAME, "Context")
//        WildcardTypeName.consumerOf(context)
        return context
    }

    private fun getJavaxInject(): ClassName {
        val context = ClassName(JAVAX_INJECT_PACKAGE_NAME, "Inject")
        return context
    }

    private fun getDaggerHiltQualifierPackageName(): ClassName {
        return ClassName(DAGGER_HILT_QUALIFIER_PACKAGE_NAME, "ApplicationContext")
    }
    private fun getDeepLinkDestination(): ClassName {
        return ClassName(DEEPLINK_INTERFACE, "DeeplinkProcessor")
    }

    private fun getContentIntentMemberName(): MemberName {
        return MemberName(ANDROID_CONTENT_PACKAGE_NAME, "Intent")
    }

}

