package com.serebit.autotitan.annotations

import net.dv8tion.jda.core.Permission

@Target(AnnotationTarget.FUNCTION)
annotation class GuildCommandFunction(
    val name: String = "",
    val description: String = "",
    val delimitFinalParameter: Boolean = true,
    val permissions: Array<Permission> = arrayOf()
)