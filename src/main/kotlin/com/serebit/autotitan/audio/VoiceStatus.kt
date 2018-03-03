package com.serebit.autotitan.audio

import net.dv8tion.jda.core.entities.MessageChannel

enum class VoiceStatus(val errorMessage: String?) {
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
}
