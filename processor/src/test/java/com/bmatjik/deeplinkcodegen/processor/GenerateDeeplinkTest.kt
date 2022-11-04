package com.bmatjik.deeplinkcodegen.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


class GenerateDeeplinkTest {

    private fun fake() =ClassName("com.dummy.bmatjik", "CsdActivity")

    @Test
    fun test() {

        GenerateDeeplink.execute("test", "com.bmatjik", "CsdActivity",fake()).apply {
            println(this)
        }
    }
}