@file:JvmName("JdaExtensions")

package com.serebit.extensions.jda

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.User

inline fun jda(accountType: AccountType, init: JDABuilder.() -> Unit): JDA =
    JDABuilder(accountType).apply(init).buildBlocking()

fun JDA.getUserByMention(mention: String): User? = getUserById(
    mention
        .removeSurrounding("<@", ">")
        .removePrefix("!")
)

fun JDA.getEmoteByMention(mention: String): Emote? = getEmoteById(
    mention
        .removeSurrounding("<", ">")
        .replace(":\\S+:".toRegex(), "")
)
