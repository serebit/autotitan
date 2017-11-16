package com.serebit.autotitan.api.meta.annotations

/**
 * Represents a bot extension.
 *
 * @param name The string used to identify the extension. If left empty, uses the class
 * name instead.
 */
@Target(AnnotationTarget.CLASS)
annotation class Module(
        val name: String = ""
)
