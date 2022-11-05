package com.bmatjik.deeplinkcodegen.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Destination(val alias:String ="")
