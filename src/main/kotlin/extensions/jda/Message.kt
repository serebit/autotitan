package com.serebit.autotitan.extensions.jda

import com.serebit.autotitan.data.Emote
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.RestAction

val Message.mentionsUsers get() = mentionedUsers.isNotEmpty() || mentionedMembers.isNotEmpty() || mentionsEveryone()

fun Message.addReaction(emote: Emote): RestAction<Void> = if (emote.isDiscordEmote) addReaction(
    jda.getEmoteById(emote.emoteIdValue!!)!!
) else addReaction(emote.unicodeValue!!)
