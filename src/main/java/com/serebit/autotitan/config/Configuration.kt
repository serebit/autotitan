package com.serebit.autotitan.config

import net.dv8tion.jda.core.entities.User

data class Configuration(
    val token: String,
    val prefix: String,
    val blackList: Set<User> = mutableSetOf()
)
