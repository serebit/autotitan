package com.serebit.extensions.jda

import net.dv8tion.jda.core.audio.hooks.ConnectionListener
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.managers.AudioManager

inline fun AudioManager.onConnectionStatusChange(crossinline task: (ConnectionStatus) -> Unit) {
    connectionListener = object : ConnectionListener {
        override fun onStatusChange(status: ConnectionStatus) {
            task(status)
            connectionListener = null
        }

        override fun onUserSpeaking(user: User?, speaking: Boolean) = Unit

        override fun onPing(ping: Long) = Unit
    }
}

inline fun AudioManager.openAudioConnection(channel: VoiceChannel, crossinline onConnect: () -> Unit) {
    openAudioConnection(channel)
    onConnectionStatusChange { status ->
        if (status == ConnectionStatus.CONNECTED) {
            onConnect()
        }
    }
}
