package com.serebit.autotitan.api

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel

inline class LongString(val value: String) {
    override fun toString() = value
}

sealed class Emote {
    abstract fun canInteract(channel: MessageChannel): Boolean

    abstract fun asMention(guild: Guild): String

    data class Jda(val id: Long) : Emote() {
        override fun canInteract(channel: MessageChannel) =
            channel.jda.getEmoteById(id).canInteract(channel.jda.selfUser, channel, true)

        override fun asMention(guild: Guild): String = guild.getEmoteById(id).asMention
    }

    data class Unicode(val unicode: String) : Emote() {
        override fun canInteract(channel: MessageChannel) = true

        override fun asMention(guild: Guild): String = unicode
    }

    companion object {
        private val emojiRegex = "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+".toRegex()
        private val String.isUnicodeEmoji
            get() = matches(emojiRegex)

        fun from(string: String, guild: Guild? = null): Emote? = if (string.isUnicodeEmoji) {
            Unicode(string)
        } else guild?.getEmoteByMention(string)?.let { Jda(it.idLong) }

        private fun Guild.getEmoteByMention(mention: String): net.dv8tion.jda.core.entities.Emote? =
            getEmoteById(mention.removeSurrounding("<", ">").replace(":\\S+:".toRegex(), ""))
    }
}
