package com.serebit.autotitan.extensions

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User

private val ownerMap = mutableMapOf<JDA, User>()

val User.isBotOwner
    get() = this == ownerMap.getOrPut(jda) {
        jda.retrieveApplicationInfo().complete().owner
    }
