package com.serebit.autotitan.api.parameters

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.data.classpathResource
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Emote as JdaEmote

sealed class Emote {
    abstract fun canInteract(channel: MessageChannel): Boolean

    abstract fun asMention(jda: JDA): String

    data class Jda(private val id: Long) : Emote() {
        val value: Long = id

        override fun canInteract(channel: MessageChannel) =
            channel.jda.getEmoteById(id).canInteract(channel.jda.selfUser, channel, true)

        override fun asMention(jda: JDA): String = jda.getEmoteById(id).asMention
    }

    data class Unicode(private val unicode: String) : Emote() {
        val value: String = unicode

        override fun canInteract(channel: MessageChannel) = true

        override fun asMention(jda: JDA): String = unicode
    }

    companion object {
        private val emojiCodePoints = Gson().fromJson<Set<IntArray>>(
            classpathResource("resources/emoji-code-points.json").readText()
        )
        private val String.isUnicodeEmoji
            get() = codePoints().toArray().let { codePoints ->
                emojiCodePoints.any { it.contentEquals(codePoints) }
            }

        fun from(string: String, jda: JDA? = null): Emote? = if (string.isUnicodeEmoji) {
            Unicode(string)
        } else jda?.getEmoteByMention(string)?.let { Jda(it.idLong) }

        private fun JDA.getEmoteByMention(mention: String): JdaEmote? =
            getEmoteById(mention.removeSurrounding("<", ">").replace(":\\S+:".toRegex(), ""))
    }
}
