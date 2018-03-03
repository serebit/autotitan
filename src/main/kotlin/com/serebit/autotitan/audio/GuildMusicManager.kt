package com.serebit.autotitan.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import net.dv8tion.jda.core.audio.AudioSendHandler

class GuildMusicManager : AudioEventAdapter() {
    private val player: AudioPlayer = AudioHandler.createPlayer().also {
        it.addListener(this)
    }
    val queue = mutableListOf<AudioTrack>()
    val sendHandler = AudioPlayerSendHandler()
    var volume: Int
        get() = player.volume
        set(value) {
            player.volume = value.coerceIn(0..maxVolume)
        }
    val playingTrack: AudioTrack? get() = player.playingTrack

    fun reset() {
        resume()
        stop()
        player.volume = maxVolume
    }

    fun addToQueue(track: AudioTrack) {
        player.playingTrack?.let {
            queue.add(track)
        } ?: player.playTrack(track)
    }

    fun skipTrack() {
        player.playingTrack?.let {
            player.stopTrack()
            if (queue.isNotEmpty()) player.playTrack(queue.removeAt(0))
        }
    }

    fun pause() {
        player.isPaused = true
    }

    fun resume() {
        player.isPaused = false
    }

    fun stop() {
        player.stopTrack()
        queue.clear()
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (queue.isNotEmpty() && endReason == AudioTrackEndReason.FINISHED) {
            player.playTrack(queue.removeAt(0))
        }
    }

    inner class AudioPlayerSendHandler : AudioSendHandler {
        // behavior is the same whether we check for frames or not, so always return true
        override fun canProvide(): Boolean = true

        override fun provide20MsAudio(): ByteArray? = player.provide()?.data

        override fun isOpus() = true
    }

    companion object {
        private const val maxVolume = 100
    }
}
