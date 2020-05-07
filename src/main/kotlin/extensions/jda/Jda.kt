package com.serebit.autotitan.extensions.jda

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent

inline fun jda(token: String, init: JDABuilder.() -> Unit): JDA =
    JDABuilder.create(token, GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS)).apply(init).build().awaitReady()

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
