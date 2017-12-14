package com.serebit.extensions.jda

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.requests.RestAction

inline fun MessageChannel.sendEmbed(init: EmbedBuilder.() -> Unit): RestAction<Message> = sendMessage(embed(init))