package com.bmatjik.deeplinkcodegen.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


class GenerateDeeplinkTest {

    private fun fake() =ClassName("com.dummy.bmatjik", "CsdActivity")
    @Test
    fun `Generated Class DeeplinkDestination`() {
        GenerateDeeplink.execute("test", "com.bmatjik", "Home","HomeActivity").apply {
            println(this)
        }
    }

    @Test
    fun `Generated Class Dagger Extend for DeeplinkDestination`() {
        GenerateDeeplink.executeDagger("testModule", "com.bmatjik", "HomeDestinationModule","HomeDestination").apply {
            println(this)
        }
    }
}