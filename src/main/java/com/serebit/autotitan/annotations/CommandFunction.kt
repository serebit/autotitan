package com.serebit.autotitan.annotations

import net.dv8tion.jda.core.Permission

@Target(AnnotationTarget.FUNCTION)
annotation class CommandFunction(
    val name: String = "",
    val description: String = "",
    val delimitFinalParameter: Boolean = true,
    val serverOnly: Boolean = false,
    val botPermissions: Array<Permission> = arrayOf(),
    val userPermissions: Array<Permission> = arrayOf()
)
