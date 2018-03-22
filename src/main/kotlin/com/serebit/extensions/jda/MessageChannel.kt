@file:JvmName("MessageChannelExtensions")

package com.serebit.extensions.jda

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.requests.RestAction
import net.dv8tion.jda.core.requests.restaction.MessageAction

fun MessageChannel.sendEmbed(init: EmbedBuilder.() -> Unit): RestAction<Message> {
    return sendMessage(EmbedBuilder().apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
        init()
    }.build())
}

fun MessageChannel.sendEmbed(embedBuilder: EmbedBuilder): MessageAction {
    return sendMessage(embedBuilder.apply {
        setColor((this@sendEmbed as? TextChannel)?.guild?.selfMember?.color)
    }.build())
}
