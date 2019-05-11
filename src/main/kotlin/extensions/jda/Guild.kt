package com.serebit.autotitan.extensions.jda

import com.serebit.autotitan.audio.GuildTrackManager
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel

private val trackManagers = mutableMapOf<Long, GuildTrackManager>()

val Guild.trackManager: GuildTrackManager
    get() = trackManagers.getOrPut(idLong) {
        GuildTrackManager().also {
            audioManager.sendingHandler = it.sendHandler
        }
    }

fun Guild.getMemberByMention(mention: String): Member? = getMemberById(
    mention.removeSurrounding("<@", ">")
        .removePrefix("!")
)

fun Guild.getTextChannelByMention(mention: String): TextChannel? = getTextChannelById(
    mention.removeSurrounding("<#", ">")
)
