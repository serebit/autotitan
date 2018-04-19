package com.serebit.autotitan.data

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.extensions.jda.getEmoteByMention
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.MessageChannel

data class Emote(val unicodeValue: String? = null, val emoteIdValue: EmoteId? = null) {
    val isDiscordEmote get() = emoteIdValue != null
    val isUnicodeEmote get() = unicodeValue != null

    fun canInteract(channel: MessageChannel) = if (isDiscordEmote) {
        channel.jda.getEmoteById(emoteIdValue!!).canInteract(channel.jda.selfUser, channel, true)
    } else true

    fun toString(jda: JDA): String {
        return if (isDiscordEmote) jda.getEmoteById(emoteIdValue!!).asMention else unicodeValue!!
    }

    companion object {
        private val emojiCodePoints = Gson().fromJson<Set<IntArray>>(
            DataManager.getResource("/emoji-code-points.json").readText()
        )

        private val String.isUnicodeEmoji
            get() = codePoints().toArray().let { codePoints ->
                emojiCodePoints.any { it.contentEquals(codePoints) }
            }

        fun from(string: String, jda: JDA? = null): Emote? = if (string.isUnicodeEmoji) {
            Emote(string)
        } else {
            jda?.getEmoteByMention(string)?.let { Emote(emoteIdValue = it.idLong) }
        }
    }
}
