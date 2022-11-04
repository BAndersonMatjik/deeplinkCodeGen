package com.bmatjik.deeplinkcodegen.annotations

interface DeepLinkDestination {
    fun matches(deeplink: String): Boolean

    fun execute(deeplink: String)

    companion object
}