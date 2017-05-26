package com.serebit.autotitan.extensions.standard.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ListenerFunction
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.managers.AudioManager
import org.apache.commons.validator.routines.UrlValidator

class Audio {
  private val urlValidator = UrlValidator(arrayOf("http", "https"))
  private val playerManager = DefaultAudioPlayerManager()
  private val audioManagers = mutableMapOf<Guild, GuildMusicManager>()

  init {
    AudioSourceManagers.registerRemoteSources(playerManager)
    AudioSourceManagers.registerLocalSource(playerManager)
  }

  @CommandFunction(locale = Locale.GUILD)
  fun joinVoice(evt: MessageReceivedEvent) {
    if (evt.member.voiceState.inVoiceChannel()) {
      connectToVoiceChannel(evt.guild.audioManager, evt.member.voiceState.channel)
    } else {
      evt.channel.sendMessage("You need to be in a voice channel for me to do that.").queue()
    }
  }

  @CommandFunction(locale = Locale.GUILD)
  fun leaveVoice(evt: MessageReceivedEvent) {
    if (evt.guild.audioManager.isConnected) {
      val audioPlayer = evt.guild.getMusicManager()
      audioPlayer.scheduler.next()
      evt.guild.audioManager.closeAudioConnection()
    }
  }

  @ListenerFunction()
  fun leaveVoiceAutomatically(evt: GuildVoiceLeaveEvent) {
    if (evt.guild.audioManager.connectedChannel != evt.channelLeft) return
    val nobodyLeft = evt.guild.audioManager.connectedChannel.members.size == 1
    if (evt.guild.audioManager.isConnected && nobodyLeft) {
      val audioPlayer = evt.guild.getMusicManager()
      audioPlayer.scheduler.stop()
      evt.guild.audioManager.closeAudioConnection()
    }
  }

  @CommandFunction(
      locale = Locale.GUILD,
      delimitFinalParameter = false
  )
  fun play(evt: MessageReceivedEvent, linkOrSearchTerms: String) {
    if (evt.guild.audioManager.isConnected) {
      val audioManager = evt.guild.getMusicManager()
      var formattedLinkOrSearchTerms = if (urlValidator.isValid(linkOrSearchTerms)) {
        linkOrSearchTerms
      } else {
        "ytsearch:$linkOrSearchTerms"
      }
      playerManager.loadItemOrdered(audioManager, formattedLinkOrSearchTerms, object : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
          evt.channel.sendMessage("Adding ${track.info.title} to queue.").queue()
          audioManager.scheduler.queue(track)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
          var firstTrack = playlist.selectedTrack ?: playlist.tracks[0]
          evt.channel.sendMessage("Adding ${firstTrack.info.title} to queue.").queue()
          audioManager.scheduler.queue(firstTrack)
        }

        override fun noMatches() {
          evt.channel.sendMessage("Nothing found.").queue()
        }

        override fun loadFailed(exception: FriendlyException) {
          evt.channel.sendMessage("Could not queue: ${exception.message}").queue()
        }
      })
    } else {
      evt.channel.sendMessage("I need to be in a voice channel to do that.").queue()
    }
  }

  @CommandFunction(locale = Locale.GUILD)
  fun skip(evt: MessageReceivedEvent) {
    val audioManager = evt.guild.getMusicManager()
    audioManager.scheduler.next()
    evt.channel.sendMessage("Skipped to next track.").queue()
  }
  
  @CommandFunction(locale = Locale.GUILD)
  fun pause(evt: MessageReceivedEvent) {
    val audioManager = evt.guild.getMusicManager()
    if (audioManager.scheduler.pause()) {
      evt.channel.sendMessage("Paused.").queue()
    }
  }
  
  @CommandFunction(locale = Locale.GUILD)
  fun resume(evt: MessageReceivedEvent) {
    val audioManager = evt.guild.getMusicManager()
    if (audioManager.scheduler.resume()) {
      evt.channel.sendMessage("Resumed.").queue()
    }
  }

  @CommandFunction(locale = Locale.GUILD)
  fun queue(evt: MessageReceivedEvent) {
    if (evt.guild.getMusicManager().player.playingTrack != null) {
      val audioManager = evt.guild.getMusicManager()
      val embedBuilder = EmbedBuilder()
      embedBuilder
          .setTitle("Queue", null)
          .setColor(evt.guild.getMember(evt.jda.selfUser).color)
          .setThumbnail(evt.jda.selfUser.effectiveAvatarUrl)
          .addField(
              "Now Playing",
              audioManager.player.playingTrack.info.title,
              false
          )
      if (audioManager.scheduler.queue.isNotEmpty()) {
        embedBuilder.addField(
            "Up Next",
            audioManager.scheduler.queue.map { it.info.title }.joinToString("\n"),
            false
        )
      }
      evt.channel.sendMessage(embedBuilder.build()).queue()
    } else {
      evt.channel.sendMessage("There's nothing queued at the moment.").queue()
    }
  }

  @CommandFunction(locale = Locale.GUILD)
  fun volume(evt: MessageReceivedEvent, volume: Int) {
    val newVolume = when {
      volume > 100 -> 100
      volume < 0 -> 0
      else -> volume
    }
    evt.guild.getMusicManager().player.volume = newVolume
    evt.channel.sendMessage("Set volume to $newVolume.").queue()
  }

  fun Guild.getMusicManager(): GuildMusicManager {
    var audioManager = audioManagers.getOrElse(this, {
      val newManager = GuildMusicManager(playerManager)
      audioManagers.put(this, newManager)
      newManager
    })
    this.audioManager.sendingHandler = audioManager.sendHandler
    return audioManager
  }

  private fun connectToVoiceChannel(audioManager: AudioManager, voiceChannel: VoiceChannel) {
    if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
      audioManager.openAudioConnection(voiceChannel)
    }
  }
}