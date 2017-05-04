package com.serebit.autotitan.extensions

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import java.util.concurrent.LinkedBlockingQueue

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
class TrackScheduler
/**
 * @param player The audio player this scheduler uses
 */
(private val player: AudioPlayer) : AudioEventAdapter() {
  private val queue = LinkedBlockingQueue<AudioTrack>()

  fun queue(track: AudioTrack) {
    if (!player.startTrack(track, true)) {
      queue.offer(track)
    }
  }

  fun nextTrack() {
    player.startTrack(queue.poll(), false)
  }

  override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
    if (endReason.mayStartNext) {
      nextTrack()
    }
  }
}
