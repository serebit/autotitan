package com.serebit.autotitan.extensions.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.serebit.autotitan.Locale
import com.serebit.autotitan.annotations.CommandFunction
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.managers.AudioManager

class Audio {
  private val playerManager = DefaultAudioPlayerManager()
  private val audioManagers = mutableMapOf<Long, GuildMusicManager>()

  init {
    AudioSourceManagers.registerRemoteSources(playerManager)
    AudioSourceManagers.registerLocalSource(playerManager)
  }

  @CommandFunction(locale = Locale.GUILD)
  fun joinVoice(evt: MessageReceivedEvent) {
    if (evt.member.voiceState.inVoiceChannel()) {
      connectToVoiceChannel(evt.guild.audioManager, evt.member.voiceState.channel)
    }
  }

  @CommandFunction(locale = Locale.GUILD)
  fun leaveVoice(evt: MessageReceivedEvent) {
    if (evt.guild.audioManager.isConnected) {
      evt.guild.audioManager.closeAudioConnection()
    }
  }

  @CommandFunction(locale = Locale.GUILD)
  fun play(evt: MessageReceivedEvent, link: String) {
    if (evt.guild.audioManager.isConnected) {
      val audioManager = getGuildAudioPlayer(evt.guild)
      playerManager.loadItemOrdered(audioManager, link, object : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
          evt.channel.sendMessage("Adding ${track.info.title} to queue.").queue()
          audioManager.scheduler.queue(track)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
          var firstTrack = playlist.selectedTrack ?: playlist.tracks[0]
          evt.channel.sendMessage(
              "Adding ${firstTrack.info.title} to queue (first track of playlist ${playlist.name})"
          ).queue()
          audioManager.scheduler.queue(firstTrack)
        }

        override fun noMatches() {
          evt.channel.sendMessage("Nothing found at $link.").queue()
        }

        override fun loadFailed(exception: FriendlyException) {
          evt.channel.sendMessage("Could not play: ${exception.message}").queue()
        }
      })
    }
  }

  @CommandFunction(locale = Locale.GUILD)
  fun skip(evt: MessageReceivedEvent) {
    val audioManager = getGuildAudioPlayer(evt.guild)
    audioManager.scheduler.nextTrack()
    evt.channel.sendMessage("Skipped to next track.").queue()
  }

  @Synchronized private fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
    val guildId = guild.id.toLong()
    var audioManager = audioManagers.getOrElse(guildId, { GuildMusicManager(playerManager) })
    guild.audioManager.sendingHandler = audioManager.sendHandler
    return audioManager
  }

  private fun connectToVoiceChannel(audioManager: AudioManager, voiceChannel: VoiceChannel) {
    if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
      audioManager.openAudioConnection(voiceChannel)
    }
  }
}