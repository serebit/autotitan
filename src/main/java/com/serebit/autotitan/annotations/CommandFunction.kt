package com.serebit.autotitan.annotations

import com.serebit.autotitan.Access
import com.serebit.autotitan.Locale
import net.dv8tion.jda.core.Permission

@Target(AnnotationTarget.FUNCTION)
annotation class CommandFunction(
    val name: String = "",
    val description: String = "",
    val locale: Locale = Locale.ALL,
    val access: Access = Access.ALL,
    val delimitFinalParameter: Boolean = true,
    val hidden: Boolean = false,
    val permissions: Array<Permission> = arrayOf()
)
