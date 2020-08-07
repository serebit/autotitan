package com.serebit.autotitan.data

import com.serebit.autotitan.extensions.getEmoteByMention
import com.vdurmont.emoji.EmojiParser
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageChannel

@Serializable
class Emote private constructor(var unicodeValue: String? = null, var emoteIDValue: EmoteID? = null) {
    val isDiscordEmote get() = emoteIDValue != null
    val isUnicodeEmote get() = unicodeValue != null

    fun canInteract(channel: MessageChannel) = if (isDiscordEmote) {
        channel.jda.getEmoteById(emoteIDValue!!)!!.canInteract(channel.jda.selfUser, channel, true)
    } else true

    fun toString(jda: JDA): String {
        return if (isDiscordEmote) jda.getEmoteById(emoteIDValue!!)!!.asMention else unicodeValue!!
    }

    override fun equals(other: Any?): Boolean = if (other is Emote) {
        other.unicodeValue == unicodeValue && other.emoteIDValue == emoteIDValue
    } else false

    @Suppress("MagicNumber")
    override fun hashCode(): Int {
        return 37 * (unicodeValue?.hashCode() ?: emoteIDValue?.hashCode()!!)
    }

    companion object {
        fun from(string: String, jda: JDA? = null): Emote? = if (string.isUnicodeEmoji) {
            Emote(unicodeValue = string)
        } else {
            jda?.getEmoteByMention(string)?.let { Emote(emoteIDValue = it.idLong) }
        }
    }
}

private val String.isUnicodeEmoji: Boolean
    get() = EmojiParser.extractEmojis(this).size == 1 && EmojiParser.removeAllEmojis(this).isEmpty()
