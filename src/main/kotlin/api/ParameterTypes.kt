package com.serebit.autotitan.api

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel

inline class LongString(val value: String) {
    override fun toString() = value
}

sealed class Emote {
    abstract fun canInteract(channel: MessageChannel): Boolean

    abstract fun asMention(guild: Guild): String

    data class Jda(val emoteIdValue: Long) : Emote() {
        override fun canInteract(channel: MessageChannel) =
            channel.jda.getEmoteById(emoteIdValue)!!.canInteract(channel.jda.selfUser, channel, true)

        override fun asMention(guild: Guild): String = guild.getEmoteById(emoteIdValue)!!.asMention
    }

    data class Unicode(val unicodeValue: String) : Emote() {
        override fun canInteract(channel: MessageChannel) = true

        override fun asMention(guild: Guild): String = unicodeValue
    }

    companion object {
        private val emojiRegex = "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+".toRegex()
        private val String.isUnicodeEmoji
            get() = matches(emojiRegex)

        fun from(string: String, guild: Guild? = null): Emote? = if (string.isUnicodeEmoji) {
            Unicode(string)
        } else guild?.getEmoteByMention(string)?.let { Jda(it.idLong) }

        private fun Guild.getEmoteByMention(mention: String): net.dv8tion.jda.api.entities.Emote? =
            getEmoteById(mention.removeSurrounding("<", ">").replace(":\\S+:".toRegex(), ""))

        @JsonCreator
        @JvmStatic
        fun fromJson(@JsonProperty emoteIdValue: Long? = null, @JsonProperty unicodeValue: String?) =
            if (emoteIdValue != null) Jda(emoteIdValue) else if (unicodeValue != null) Unicode(unicodeValue) else null
    }
}
