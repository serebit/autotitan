@file:JvmName("MessageExtensions")

package com.serebit.extensions.jda

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.requests.restaction.MessageAction

const val MESSAGE_EMBED_MAX_FIELDS = 25

val Message.mentionsUsers get() = mentionedUsers.isNotEmpty() || mentionedMembers.isNotEmpty() || mentionsEveryone()

inline fun MessageChannel.sendEmbed(init: EmbedBuilder.() -> Unit): MessageAction =
    sendMessage(EmbedBuilder().apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
        init()
    }.build())

fun MessageChannel.sendEmbed(embedBuilder: EmbedBuilder): MessageAction =
    sendMessage(embedBuilder.apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
    }.build())
