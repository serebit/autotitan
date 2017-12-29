package com.serebit.extensions.jda

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.requests.RestAction
import java.time.OffsetDateTime

inline fun MessageChannel.sendEmbed(init: EmbedBuilder.() -> Unit): RestAction<Message> {
    return sendMessage(EmbedBuilder().apply {
        setTimestamp(OffsetDateTime.now())
        if (this is TextChannel) setColor(guild?.selfMember?.color)
        init()
    }.build())
}