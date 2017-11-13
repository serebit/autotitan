package com.serebit.autotitan.api.meta.annotations

/**
* Represents the attributes of a bot event listener.
* 
* @param name The name to use to refer to the Listener. If left empty, uses the function name
* instead.
* @param description A short string describing the listener and what it does.
*/
@Target(AnnotationTarget.FUNCTION)
annotation class ListenerFunction(
    val name: String = "",
    val description: String = ""
)
