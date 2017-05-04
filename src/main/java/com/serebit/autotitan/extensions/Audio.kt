package com.serebit.autotitan.extensions

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.serebit.autotitan.Locale
import com.serebit.autotitan.annotations.CommandFunction
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.managers.AudioManager

class Audio {
  private val playerManager = DefaultAudioPlayerManager()
  private val musicManagers = mutableMapOf<Long, GuildMusicManager>()

  init {
    AudioSourceManagers.registerRemoteSources(playerManager)
    AudioSourceManagers.registerLocalSource(playerManager)
  }

  @CommandFunction(locale = Locale.GUILD)
  fun play(evt: MessageReceivedEvent, link: String) {
    loadAndPlay(evt.textChannel, link)
  }

  @CommandFunction(locale = Locale.GUILD)
  fun skip(evt: MessageReceivedEvent) {
    skipTrack(evt.textChannel)
  }

  @Synchronized private fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
    val guildId = guild.id.toLong()
    var musicManager = musicManagers.getOrElse(guildId, { GuildMusicManager(playerManager) })
    guild.audioManager.sendingHandler = musicManager.sendHandler
    return musicManager
  }

  private fun connectToFirstVoiceChannel(audioManager: AudioManager) {
    if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
      for (voiceChannel in audioManager.guild.voiceChannels) {
        audioManager.openAudioConnection(voiceChannel)
        break
      }
    }
  }

  private fun loadAndPlay(channel: TextChannel, trackUrl: String) {
    val musicManager = getGuildAudioPlayer(channel.guild)

    playerManager.loadItemOrdered(musicManager, trackUrl, object : AudioLoadResultHandler {
      override fun trackLoaded(track: AudioTrack) {
        channel.sendMessage("Adding ${track.info.title} to queue.").queue()
        play(channel.guild, musicManager, track)
      }

      override fun playlistLoaded(playlist: AudioPlaylist) {
        var firstTrack: AudioTrack? = playlist.selectedTrack
        if (firstTrack == null) {
          firstTrack = playlist.tracks[0]
        }

        channel.sendMessage("Adding to queue " + firstTrack!!.info.title + " (first track of playlist " + playlist.name + ")").queue()
        play(channel.guild, musicManager, firstTrack)
      }

      override fun noMatches() {
        channel.sendMessage("Nothing found by " + trackUrl).queue()
      }

      override fun loadFailed(exception: FriendlyException) {
        channel.sendMessage("Could not play: " + exception.message).queue()
      }
    })
  }

  private fun play(guild: Guild, musicManager: GuildMusicManager, track: AudioTrack) {
    connectToFirstVoiceChannel(guild.audioManager)
    musicManager.scheduler.queue(track)
  }

  private fun skipTrack(channel: TextChannel) {
    val musicManager = getGuildAudioPlayer(channel.guild)
    musicManager.scheduler.nextTrack()
    channel.sendMessage("Skipped to next track.").queue()
  }
}