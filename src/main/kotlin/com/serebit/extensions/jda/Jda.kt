@file:JvmName("JdaExtensions")

package com.serebit.extensions.jda

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.User

inline fun jda(accountType: AccountType, init: JDABuilder.() -> Unit): JDA =
    JDABuilder(accountType).apply(init).buildBlocking()

fun JDA.getUserByMention(mention: String): User? = try {
    getUserById(
        mention
            .removeSurrounding("<@", ">")
            .removePrefix("!")
    )
} catch (ex: IllegalArgumentException) {
    null
}

fun JDA.getEmoteByMention(mention: String): Emote? = try {
    getEmoteById(
        mention
            .removeSurrounding("<", ">")
            .replace(":\\S+:".toRegex(), "")
    )
} catch (ex: IllegalArgumentException) {
    null
}
