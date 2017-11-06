package com.serebit.extensions.jda

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed

fun embed(init: EmbedBuilder.() -> Unit): MessageEmbed = EmbedBuilder().apply(init).build()