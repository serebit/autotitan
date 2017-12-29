package com.serebit.extensions.jda

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import java.time.OffsetDateTime

inline fun embed(init: EmbedBuilder.() -> Unit): MessageEmbed = EmbedBuilder().apply {
    setTimestamp(OffsetDateTime.now())
    init()
}.build()