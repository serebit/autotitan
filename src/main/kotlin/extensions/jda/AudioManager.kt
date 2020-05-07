package com.serebit.autotitan.extensions.jda

import net.dv8tion.jda.api.audio.hooks.ConnectionListener
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.managers.AudioManager

inline fun AudioManager.onConnectionStatusChange(desiredStatus: ConnectionStatus, crossinline task: () -> Unit) {
    connectionListener = object : ConnectionListener {
        override fun onStatusChange(status: ConnectionStatus) {
            if (status == desiredStatus) {
                task()
                connectionListener = null
            }
        }

        override fun onUserSpeaking(user: User, speaking: Boolean) = Unit

        override fun onPing(ping: Long) = Unit
    }
}

inline fun AudioManager.openAudioConnection(channel: VoiceChannel, crossinline task: () -> Unit) {
    onConnectionStatusChange(ConnectionStatus.CONNECTED, task)
    openAudioConnection(channel)
}

inline fun AudioManager.closeAudioConnection(crossinline task: () -> Unit) {
    onConnectionStatusChange(ConnectionStatus.NOT_CONNECTED, task)
    closeAudioConnection()
}
