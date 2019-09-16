package com.serebit.autotitan.extensions

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.requests.restaction.MessageAction

const val MESSAGE_EMBED_MAX_FIELDS = 25

inline fun MessageChannel.sendEmbed(init: EmbedBuilder.() -> Unit): MessageAction =
    sendMessage(EmbedBuilder().apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
        init()
    }.build())
