@file:JvmName("UserExtensions")

package com.serebit.autotitan.api.extensions.jda

import com.serebit.autotitan.config
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.User

private val ownerMap = mutableMapOf<JDA, User>()

val User.isNotBot get() = !isBot

val User.inBlacklist get() = idLong in config.blackList

val User.notInBlacklist get() = !inBlacklist

val User.isBotOwner
    get() = ownerMap.getOrPut(jda) {
        jda.asBot().applicationInfo.complete().owner
    } == this

val User.canInvokeCommands get() = isNotBot && notInBlacklist
