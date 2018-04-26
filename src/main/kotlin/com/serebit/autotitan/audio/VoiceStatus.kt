package com.serebit.autotitan.audio

import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

enum class VoiceStatus(private val errorMessage: String?) {
    NEITHER_CONNECTED("We both need to be in a voice channel for me to do that."),
    SELF_DISCONNECTED_USER_CONNECTED("I need to be in your voice channel to do that."),
    SELF_CONNECTED_USER_DISCONNECTED("You need to be in a voice channel for me to do that."),
    BOTH_CONNECTED_DIFFERENT_CHANNEL("We need to be in the same voice channel for you to do that."),
    BOTH_CONNECTED_SAME_CHANNEL(null);

    fun sendErrorMessage(channel: MessageChannel) {
        errorMessage?.let {
            channel.sendMessage(it).complete()
        }
    }

    companion object {
        fun from(evt: MessageReceivedEvent): VoiceStatus {
            val selfIsConnected = evt.guild.audioManager.isConnected
            val userIsConnected = evt.member.voiceState.inVoiceChannel()
            val differentChannel = evt.member.voiceState.channel != evt.guild.audioManager.connectedChannel
            return when {
                !userIsConnected && selfIsConnected -> VoiceStatus.SELF_CONNECTED_USER_DISCONNECTED
                !selfIsConnected && userIsConnected -> VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED
                !selfIsConnected && !userIsConnected -> VoiceStatus.NEITHER_CONNECTED
                differentChannel -> VoiceStatus.BOTH_CONNECTED_DIFFERENT_CHANNEL
                else -> VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL
            }
        }
    }
}
