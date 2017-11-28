package com.serebit.autotitan.modules.standard.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason

/**
 * Holder for both the player and a track scheduler for one guild.
 */
class GuildMusicManager(manager: AudioPlayerManager) {
    val player: AudioPlayer = manager.createPlayer()
    val scheduler = TrackScheduler()
    val sendHandler by lazy {
        AudioPlayerSendHandler(player)
    }

    init {
        player.addListener(scheduler)
    }

    inner class TrackScheduler : AudioEventAdapter() {
        val queue = mutableListOf<AudioTrack>()

        fun addToQueue(track: AudioTrack) {
            if (player.playingTrack == null) {
                player.playTrack(track)
            } else {
                queue.add(track)
            }
        }

        fun skipTrack(): Boolean {
            return if (player.playingTrack != null) {
                player.stopTrack()
                if (queue.isNotEmpty()) player.playTrack(queue.removeAt(0))
                true
            } else {
                false
            }
        }

        fun pause() = if (!player.isPaused) {
            player.isPaused = true
            true
        } else false

        fun resume() = if (player.isPaused) {
            player.isPaused = false
            true
        } else false

        fun stop() {
            player.stopTrack()
            queue.clear()
        }

        override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
            if (queue.size >= 1 && endReason == AudioTrackEndReason.FINISHED) {
                if (queue.isNotEmpty()) player.playTrack(queue.removeAt(0))
            }
        }
    }
}
