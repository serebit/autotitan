package com.serebit.autotitan.extensions.jda

import com.serebit.autotitan.config
import net.dv8tion.jda.api.entities.User

val User.isNotBot get() = !isBot

val User.inBlacklist get() = idLong in config.blackList

val User.notInBlacklist get() = !inBlacklist
