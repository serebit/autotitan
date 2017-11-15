package com.serebit.autotitan.api.meta.annotations

/**
 * Represents a bot extension.
 *
 * @param name The string used to identify the extension. If left empty, uses the class
 * name instead.
 * @param description A short string describing the extension and what it does.
 */
@Target(AnnotationTarget.CLASS)
annotation class ExtensionClass(
        val name: String = "",
        val description: String = ""
)
