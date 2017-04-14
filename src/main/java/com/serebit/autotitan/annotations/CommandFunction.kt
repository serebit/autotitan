package com.serebit.autotitan.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class CommandFunction(
    val name: String = "",
    val description: String = "",
    val delimitFinalParameter: Boolean = true,
    val serverOnly: Boolean = false
)
