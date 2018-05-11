package com.serebit.autotitan.api.parameters

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.data.FileManager
import com.serebit.extensions.jda.getEmoteByMention
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Emote as JdaEmote

private typealias EmoteId = Long

data class Emote(private val unicodeValue: String? = null, private val emoteIdValue: EmoteId? = null) {
    val value get() = unicodeValue ?: emoteIdValue
    val isDiscordEmote get() = emoteIdValue != null
    val isUnicodeEmote get() = unicodeValue != null

    fun canInteract(channel: MessageChannel) = if (isDiscordEmote) {
        channel.jda.getEmoteById(emoteIdValue!!).canInteract(channel.jda.selfUser, channel, true)
    } else true

    fun asMention(jda: JDA): String {
        return when {
            isDiscordEmote -> jda.getEmoteById(emoteIdValue!!).asMention
            isUnicodeEmote -> unicodeValue!!
            else -> ""
        }
    }

    companion object {
        private val emojiCodePoints = Gson().fromJson<Set<IntArray>>(
            FileManager.classpathResource("resources/emoji-code-points.json").readText()
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
