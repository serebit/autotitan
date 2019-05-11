package com.serebit.autotitan.data

import com.serebit.autotitan.extensions.isUnicodeEmoji
import com.serebit.autotitan.extensions.jda.getEmoteByMention
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.MessageChannel

class Emote {
    val unicodeValue: String?
    val emoteIdValue: EmoteId?
    val isDiscordEmote get() = emoteIdValue != null
    val isUnicodeEmote get() = unicodeValue != null

    private constructor(unicode: String) {
        unicodeValue = unicode
        emoteIdValue = null
    }

    private constructor(emoteId: EmoteId) {
        unicodeValue = null
        emoteIdValue = emoteId
    }

    fun canInteract(channel: MessageChannel) = if (isDiscordEmote) {
        channel.jda.getEmoteById(emoteIdValue!!).canInteract(channel.jda.selfUser, channel, true)
    } else true

    fun toString(jda: JDA): String {
        return if (isDiscordEmote) jda.getEmoteById(emoteIdValue!!).asMention else unicodeValue!!
    }

    override fun equals(other: Any?): Boolean = if (other is Emote) {
        other.unicodeValue == unicodeValue && other.emoteIdValue == emoteIdValue
    } else false

    @Suppress("MagicNumber")
    override fun hashCode(): Int {
        return 37 * (unicodeValue?.hashCode() ?: emoteIdValue?.hashCode()!!)
    }

    companion object {
        fun from(string: String, jda: JDA? = null): Emote? = if (string.isUnicodeEmoji) {
            Emote(string)
        } else {
            jda?.getEmoteByMention(string)?.let { Emote(it.idLong) }
        }
    }
}
