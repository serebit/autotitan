package com.serebit.autotitan.annotations

import com.serebit.autotitan.Locale
import net.dv8tion.jda.core.Permission

@Target(AnnotationTarget.FUNCTION)
annotation class CommandFunction(
    val name: String = "",
    val description: String = "",
    val delimitFinalParameter: Boolean = true,
    val locale: Locale = Locale.ALL,
    val permissions: Array<Permission> = arrayOf()
)
