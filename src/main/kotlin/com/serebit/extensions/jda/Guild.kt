package com.serebit.extensions.jda

import com.serebit.autotitan.audio.GuildMusicManager
import net.dv8tion.jda.core.entities.Guild

private val musicManagers = mutableMapOf<Long, GuildMusicManager>()

val Guild.musicManager: GuildMusicManager
    get() = musicManagers.getOrPut(idLong) {
        GuildMusicManager().also {
            audioManager.sendingHandler = it.sendHandler
        }
    }

