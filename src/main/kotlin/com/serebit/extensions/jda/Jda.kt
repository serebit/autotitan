@file:JvmName("JdaExtensions")

package com.serebit.extensions.jda

import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.User

fun JDA.getUserByMention(mention: String): User? = try {
    getUserById(mention.removeSurrounding("<@", ">").removePrefix("!"))
} catch (ex: IllegalArgumentException) {
    Logger.debug("Attempted to get user instance from invalid mention $mention.")
    null
}

fun JDA.getEmoteByMention(mention: String): Emote? = try {
    getEmoteById(mention.removeSurrounding("<", ">").replace(":\\S+:".toRegex(), ""))
} catch (ex: IllegalArgumentException) {
    Logger.debug("Attempted to get emote instance from invalid mention $mention.")
    null
}
