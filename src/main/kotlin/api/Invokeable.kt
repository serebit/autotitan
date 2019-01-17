package com.serebit.autotitan.api

import net.dv8tion.jda.core.entities.MessageEmbed

internal interface Invokeable {
    val helpField: MessageEmbed.Field
    val helpSignature: Regex
}
