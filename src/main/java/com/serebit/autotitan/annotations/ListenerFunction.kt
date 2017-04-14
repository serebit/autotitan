package com.serebit.autotitan.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class ListenerFunction(
    val name: String = "",
    val description: String = "",
    val serverOnly: Boolean = false
)
