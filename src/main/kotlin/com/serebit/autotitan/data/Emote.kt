package com.serebit.autotitan.data

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.MessageChannel

class Emote {
    val unicodeValue: String?
    val emoteIdValue: EmoteId?
    val isDiscordEmote get() = emoteIdValue != null
    val isUnicodeEmote get() = unicodeValue != null

    constructor(unicode: String) {
        unicodeValue = unicode
        emoteIdValue = null
    }

    constructor(emoteId: EmoteId) {
        unicodeValue = null
        emoteIdValue = emoteId
    }

    fun canInteract(channel: MessageChannel) = if (isDiscordEmote) {
        channel.jda.getEmoteById(emoteIdValue!!).canInteract(channel.jda.selfUser, channel, true)
    } else true

    override fun equals(other: Any?): Boolean {
        return if (other is Emote?) {
            other?.unicodeValue == unicodeValue && other?.emoteIdValue == emoteIdValue
        } else false
    }

    fun toString(jda: JDA): String {
        return if (isDiscordEmote) jda.getEmoteById(emoteIdValue!!).asMention else unicodeValue!!
    }
}
