package com.serebit.autotitan.api.parameters

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Emote as JdaEmote

sealed class Emote {
    abstract fun canInteract(channel: MessageChannel): Boolean

    abstract fun asMention(guild: Guild): String

    data class Jda(private val id: Long) : Emote() {
        val value: Long = id

        override fun canInteract(channel: MessageChannel) =
            channel.jda.getEmoteById(id).canInteract(channel.jda.selfUser, channel, true)

        override fun asMention(guild: Guild): String = guild.getEmoteById(id).asMention
    }

    data class Unicode(private val unicode: String) : Emote() {
        val value: String = unicode

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

        private fun Guild.getEmoteByMention(mention: String): JdaEmote? =
            getEmoteById(mention.removeSurrounding("<", ">").replace(":\\S+:".toRegex(), ""))
    }
}
