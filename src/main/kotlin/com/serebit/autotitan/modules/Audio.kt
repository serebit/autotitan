package com.serebit.autotitan.modules

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.api.meta.annotations.Listener
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.managers.AudioManager
import org.apache.commons.validator.routines.UrlValidator

class Audio : Module() {
    private val urlValidator = UrlValidator(arrayOf("http", "https"))
    private val playerManager = DefaultAudioPlayerManager()
    private val musicManagers = mutableMapOf<Long, GuildMusicManager>()
    private val Guild.musicManager: GuildMusicManager
        get() {
            val musicManager = musicManagers.getOrElse(idLong, {
                val newManager = GuildMusicManager(playerManager)
                musicManagers.put(idLong, newManager)
                newManager
            })
            audioManager.sendingHandler = musicManager.sendHandler
            return musicManager
        }

    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)

        command("joinVoice") { evt ->
            if (evt.member.voiceState.inVoiceChannel()) {
                connectToVoiceChannel(evt.guild.audioManager, evt.member.voiceState.channel)
                evt.channel.sendMessage("Now connected to ${evt.member.voiceState.channel.name}.").complete()
            } else {
                evt.channel.sendMessage("You need to be in a voice channel for me to do that.").complete()
            }
        }

        command("leaveVoice") { evt ->
            if (evt.guild.audioManager.isConnected) {
                evt.guild.musicManager.scheduler.stop()
                evt.guild.audioManager.closeAudioConnection()
            }
        }

        command("play") { evt, query: String ->
            val voiceStatus = voiceStatus(evt)
            when (voiceStatus) {
                VoiceStatus.CONNECTED_DIFFERENT_CHANNEL, VoiceStatus.USER_NOT_CONNECTED -> {
                    voiceStatus.sendErrorMessage(evt.channel)
                    return@command
                }
                VoiceStatus.SELF_NOT_CONNECTED -> {
                    connectToVoiceChannel(evt.guild.audioManager, evt.member.voiceState.channel)
                }
                else -> Unit
            }
            val audioManager = evt.guild.musicManager
            val formattedQuery = if (urlValidator.isValid(query)) {
                query
            } else {
                "ytsearch:$query"
            }
            playerManager.loadItemOrdered(audioManager, formattedQuery, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    evt.channel.sendMessage("Adding ${track.info.title} to queue.").complete()
                    audioManager.scheduler.addToQueue(track)
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    if (playlist.isSearchResult) {
                        val track = playlist.tracks[0]
                        audioManager.scheduler.addToQueue(track)
                        evt.channel.sendMessage("Adding ${track.info.title} to queue.").complete()
                    } else {
                        evt.channel.sendMessage(
                                "Adding ${playlist.tracks.size} songs from ${playlist.name} to queue."
                        ).complete()
                        playlist.tracks.forEach { audioManager.scheduler.addToQueue(it) }
                    }
                }

                override fun noMatches() {
                    evt.channel.sendMessage("Nothing found.").complete()
                }

                override fun loadFailed(exception: FriendlyException) {
                    evt.channel.sendMessage("Could not queue: ${exception.message}").complete()
                }
            })
        }

        command("skip") { evt ->
            voiceStatus(evt).let {
                if (it != VoiceStatus.CONNECTED_SAME_CHANNEL) {
                    it.sendErrorMessage(evt.channel)
                    return@command
                }
            }
            val audioManager = evt.guild.musicManager
            if (audioManager.scheduler.queue.isEmpty() && audioManager.player.playingTrack == null) {
                evt.channel.sendMessage("Cannot skip. Nothing is playing.").complete()
                return@command
            }
            audioManager.scheduler.skipTrack()
            evt.channel.sendMessage("Skipped to next track.").complete()
        }

        command("stop") { evt ->
            evt.guild.musicManager.scheduler.stop()
            evt.channel.sendMessage("Cleared the music queue.").complete()
        }

        command("pause") { evt ->
            voiceStatus(evt).let {
                if (it != VoiceStatus.CONNECTED_SAME_CHANNEL) {
                    it.sendErrorMessage(evt.channel)
                    return@command
                }
            }
            val audioManager = evt.guild.musicManager
            if (audioManager.scheduler.pause()) {
                evt.channel.sendMessage("Paused.").complete()
            }
        }

        command("resume") { evt ->
            voiceStatus(evt).let {
                if (it != VoiceStatus.CONNECTED_SAME_CHANNEL) {
                    it.sendErrorMessage(evt.channel)
                    return@command
                }
            }
            if (evt.guild.musicManager.scheduler.resume()) {
                evt.channel.sendMessage("Resumed.").complete()
            }
        }

        command("queue") { evt ->
            evt.run {
                if (guild.musicManager.player.playingTrack == null) {
                    channel.sendMessage("No songs are queued.").complete()
                    return@command
                }
                val audioManager = guild.musicManager

                channel.sendEmbed {
                    setAuthor(guild.selfMember.effectiveName, null, jda.selfUser.effectiveAvatarUrl)
                    setTitle("Music Queue", null)
                    val playingTrack = audioManager.player.playingTrack
                    val position = toHumanReadableDuration(playingTrack.position)
                    val duration = toHumanReadableDuration(playingTrack.duration)
                    val upNextList = audioManager.scheduler.queue.take(8).joinToString("\n") {
                        "${it.info.title} (${toHumanReadableDuration(it.duration)})"
                    }
                    addField(
                            "Now Playing",
                            "${playingTrack.info.title} ($position/$duration)",
                            false
                    )
                    if (audioManager.scheduler.queue.isNotEmpty()) addField(
                            "Up Next",
                            upNextList + if (audioManager.scheduler.queue.size > 8) {
                                "\n plus ${audioManager.scheduler.queue.drop(8).size} more..."
                            } else "",
                            false
                    )
                }.complete()
            }
        }

        command("setVolume") { evt, volume: Int ->
            voiceStatus(evt).let {
                if (it != VoiceStatus.CONNECTED_SAME_CHANNEL) {
                    it.sendErrorMessage(evt.channel)
                    return@command
                }
            }
            evt.guild.musicManager.player.volume = volume.coerceIn(0..100)
            evt.channel.sendMessage("Set volume to ${evt.guild.musicManager.player.volume}%.").complete()
        }

        listener { evt: GuildVoiceLeaveEvent ->
            evt.run {
                if (guild.audioManager.connectedChannel != channelLeft) return@listener
                if (guild.audioManager.connectedChannel.members.any { !it.user.isBot }) return@listener
                if (guild.audioManager.isConnected) {
                    guild.musicManager.scheduler.stop()
                    guild.audioManager.closeAudioConnection()
                }
            }
        }

        listener { evt: GuildVoiceMoveEvent ->
            evt.run {
                if (guild.audioManager.connectedChannel != channelLeft) return@listener
                if (guild.audioManager.connectedChannel.members.any { !it.user.isBot }) return@listener
                if (guild.audioManager.isConnected) {
                    guild.musicManager.scheduler.stop()
                    guild.audioManager.closeAudioConnection()
                }
            }
        }
    }

    private fun connectToVoiceChannel(audioManager: AudioManager, voiceChannel: VoiceChannel) {
        if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
            audioManager.openAudioConnection(voiceChannel)
        }
    }

    private fun voiceStatus(evt: MessageReceivedEvent): VoiceStatus {
        evt.run {
            val selfIsConnected = guild.audioManager.isConnected
            val userIsConnected = member.voiceState.inVoiceChannel()
            val sameChannel = member.voiceState.channel == guild.audioManager.connectedChannel
            return when {
                !userIsConnected -> VoiceStatus.USER_NOT_CONNECTED
                !selfIsConnected -> VoiceStatus.SELF_NOT_CONNECTED
                !sameChannel -> VoiceStatus.CONNECTED_DIFFERENT_CHANNEL
                else -> VoiceStatus.CONNECTED_SAME_CHANNEL
            }
        }
    }

    private enum class VoiceStatus(val errorMessage: String?) {
        SELF_NOT_CONNECTED("I need to be in a voice channel to do that."),
        USER_NOT_CONNECTED("You need to be in a voice channel for me to do that."),
        CONNECTED_DIFFERENT_CHANNEL("We need to be in the same voice channel for you to do that."),
        CONNECTED_SAME_CHANNEL(null);

        fun sendErrorMessage(channel: MessageChannel) {
            errorMessage?.let { channel.sendMessage(it).complete() }
        }
    }

    private class GuildMusicManager(manager: AudioPlayerManager) {

        val player: AudioPlayer = manager.createPlayer()
        val scheduler = TrackScheduler()
        val sendHandler by lazy {
            AudioPlayerSendHandler()
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

            fun skipTrack() {
                if (player.playingTrack != null) {
                    player.stopTrack()
                    if (queue.isNotEmpty()) player.playTrack(queue.removeAt(0))
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
                if (queue.isNotEmpty() && endReason == AudioTrackEndReason.FINISHED) {
                    player.playTrack(queue.removeAt(0))
                }
            }

        }

        inner class AudioPlayerSendHandler : AudioSendHandler {

            private var lastFrame: AudioFrame? = null
            override fun canProvide(): Boolean {
                if (lastFrame == null) lastFrame = player.provide()
                return lastFrame != null
            }

            override fun provide20MsAudio(): ByteArray? {
                if (lastFrame == null) lastFrame = player.provide()
                val data = if (lastFrame != null) lastFrame?.data else null
                lastFrame = null
                return data
            }

            override fun isOpus() = true

        }
    }

    private fun toHumanReadableDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = (totalSeconds % 3600 / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        return when (hours) {
            0 -> String.format("%d:%02d", minutes, seconds)
            else -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        }
    }
}
