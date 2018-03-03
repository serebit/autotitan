package com.serebit.extensions.jda

import com.serebit.autotitan.audio.VoiceStatus
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

val MessageReceivedEvent.voiceStatus: VoiceStatus
    get() {
        val selfIsConnected = guild.audioManager.isConnected
        val userIsConnected = member.voiceState.inVoiceChannel()
        val differentChannel = member.voiceState.channel != guild.audioManager.connectedChannel
        return when {
            !userIsConnected && selfIsConnected -> VoiceStatus.SELF_CONNECTED_USER_DISCONNECTED
            !selfIsConnected && userIsConnected -> VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED
            !selfIsConnected && !userIsConnected -> VoiceStatus.NEITHER_CONNECTED
            differentChannel -> VoiceStatus.BOTH_CONNECTED_DIFFERENT_CHANNEL
            else -> VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL
        }
    }
