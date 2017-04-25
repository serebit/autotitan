package com.serebit.autotitan.annotations

import com.serebit.autotitan.Access
import net.dv8tion.jda.core.Permission

@Target(AnnotationTarget.FUNCTION)
annotation class CommandFunction(
    val name: String = "",
    val description: String = "",
    val delimitFinalParameter: Boolean = true,
    val access: Access = Access.ALL,
    vararg val permissions: Permission = arrayOf()
)
