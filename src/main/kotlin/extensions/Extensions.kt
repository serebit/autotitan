package com.serebit.autotitan.extensions

import com.serebit.autotitan.config
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.restaction.MessageAction

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

inline fun MessageChannel.sendEmbed(init: EmbedBuilder.() -> Unit): MessageAction =
    sendMessage(EmbedBuilder().apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
        init()
    }.build())

fun MessageChannel.sendEmbed(embedBuilder: EmbedBuilder): MessageAction =
    sendMessage(embedBuilder.apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
    }.build())

fun String.truncate(max: Int): String {
    val trimmedString = replace("(\\s){2,}".toRegex(), "$1$1")
    return if (trimmedString.length > max) {
        trimmedString
            .replace("(\\s){2,}".toRegex(), "$1$1")
            .substring(0 until MessageEmbed.VALUE_MAX_LENGTH - 1) + '\u2026'
    } else trimmedString
}

inline fun <T> Iterable<T>.chunkedBy(
    size: Int,
    maxChunkSize: Int = Int.MAX_VALUE,
    transform: (T) -> Int
): List<List<T>> {
    val zipped = toList().zip(toMutableList().map(transform))
    val list = mutableListOf(mutableListOf<T>())
    var accumulator = 0
    zipped.forEach { (item, itemSize) ->
        when {
            accumulator + itemSize <= size && list.last().size < maxChunkSize -> {
                accumulator += itemSize
                list.last().add(item)
            }
            itemSize <= size -> {
                accumulator = itemSize
                list.add(mutableListOf(item))
            }
            else -> {
                accumulator = 0
                list.add(mutableListOf())
            }
        }
    }
    return list.toList()
}

val User.isNotBot get() = !isBot

val User.inBlacklist get() = idLong in config.blackList

val User.notInBlacklist get() = !inBlacklist
