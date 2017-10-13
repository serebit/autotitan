package com.serebit.autotitan.modules.standard.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason

class TrackScheduler(private var player: AudioPlayer) : AudioEventAdapter() {
    val queue = mutableListOf<AudioTrack>()

    fun queue(track: AudioTrack) {
        if (!player.startTrack(track, true)) queue.add(track)
    }

    fun next() {
        player.stopTrack()
        if (queue.isNotEmpty()) player.playTrack(queue.removeAt(0))
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
            next()
        }
    }
}
