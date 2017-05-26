package com.serebit.autotitan.extensions.standard.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason

class TrackScheduler(internal var player: AudioPlayer) : AudioEventAdapter() {
  val queue = mutableListOf<AudioTrack>()

  fun queue(track: AudioTrack) {
    if (!player.startTrack(track, true)) {
      queue.add(track)
    }
  }

  fun next() {
    player.stopTrack()
    if (queue.isNotEmpty()) {
      player.playTrack(queue[0])
      queue.removeAt(0)
    }
  }
  
  fun pause(): Boolean {
    return if (!player.isPaused): Boolean {
      player.setPaused(true)
      true
    } else false
  }
  
  fun resume(): Boolean {
    return if (player.isPaused) {
      player.setPaused(false)
      true
    } else false
  }

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
