package com.serebit.autotitan.extensions.jda

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.requests.restaction.MessageAction

inline fun MessageChannel.sendEmbed(init: EmbedBuilder.() -> Unit): MessageAction =
    sendMessage(EmbedBuilder().apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
        init()
    }.build())

fun MessageChannel.sendEmbed(embedBuilder: EmbedBuilder): MessageAction =
    sendMessage(embedBuilder.apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
    }.build())
