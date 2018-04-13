package com.serebit.autotitan.api.meta

import net.dv8tion.jda.core.Permission

data class Restrictions(
    val access: Access = Access.ALL,
    val permissions: List<Permission> = emptyList(),
    val hidden: Boolean = false
)
