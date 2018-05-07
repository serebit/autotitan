@file:JvmName("AudioExtensions")

package com.serebit.extensions.jda

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState
import com.serebit.extensions.toBasicTimestamp
import net.dv8tion.jda.core.audio.hooks.ConnectionListener
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.managers.AudioManager

private const val MILLISECONDS_PER_SECOND = 1000

val AudioTrack.infoString: String
    get() {
        val durationString = (duration / MILLISECONDS_PER_SECOND).toBasicTimestamp()
        return if (state == AudioTrackState.PLAYING) {
            val positionString = (position / MILLISECONDS_PER_SECOND).toBasicTimestamp()
            "[${info.title}](${info.uri}) [$positionString/$durationString]"
        } else {
            "[${info.title}](${info.uri}) [$durationString]"
        }
    }


inline fun AudioManager.onConnectionStatusChange(desiredStatus: ConnectionStatus, crossinline task: () -> Unit) {
    connectionListener = object : ConnectionListener {
        override fun onStatusChange(status: ConnectionStatus) {
            if (status == desiredStatus) {
                task()
                connectionListener = null
            }
        }

        override fun onUserSpeaking(user: User?, speaking: Boolean) = Unit

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
