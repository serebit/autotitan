package com.serebit.autotitan.annotations

import net.dv8tion.jda.core.Permission

@Target(AnnotationTarget.FUNCTION)
annotation class ListenerFunction(
    val name: String = "",
    val description: String = "",
    val serverOnly: Boolean = false,
    val botPermissions: Array<Permission> = arrayOf()
)
