package com.serebit.autotitan.extensions.standard.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

/**
 * Holder for both the player and a track scheduler for one guild.
 */
class GuildMusicManager(manager: AudioPlayerManager) {
  val player: AudioPlayer = manager.createPlayer()
  val scheduler = TrackScheduler(player)
  val sendHandler: AudioPlayerSendHandler
    get() = AudioPlayerSendHandler(player)

  init {
    player.addListener(scheduler)
  }
}
