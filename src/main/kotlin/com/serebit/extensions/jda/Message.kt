@file:JvmName("MessageExtensions")

package com.serebit.extensions.jda

import com.serebit.autotitan.data.Emote
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.requests.RestAction

val Message.mentionsUsers get() = mentionedUsers.isNotEmpty() || mentionedMembers.isNotEmpty() || mentionsEveryone()

fun Message.addReaction(emote: Emote): RestAction<Void>? = if (emote.isDiscordEmote) {
    jda.getEmoteById(emote.emoteIdValue!!)?.let {
        addReaction(it)
    }
} else addReaction(emote.unicodeValue!!)
