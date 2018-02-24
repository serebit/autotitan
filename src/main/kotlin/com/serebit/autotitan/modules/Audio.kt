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
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.api.meta.annotations.Listener
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.toBasicTimestamp
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.apache.commons.validator.routines.UrlValidator

@Suppress("UNUSED", "TooManyFunctions")
class Audio : Module() {
    private val urlValidator = UrlValidator(arrayOf("http", "https"))
    private val playerManager = DefaultAudioPlayerManager()
    private val musicManagers = mutableMapOf<Long, GuildMusicManager>()
    private val Guild.musicManager: GuildMusicManager
        get() {
            val musicManager = musicManagers.getOrElse(idLong, {
                val newManager = GuildMusicManager(playerManager)
                musicManagers[idLong] = newManager
                newManager
            })
            audioManager.sendingHandler = musicManager.sendHandler
            return musicManager
        }

    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    @Command(description = "Joins the voice channel that the invoker is in.", locale = Locale.GUILD)
    fun joinVoice(evt: MessageReceivedEvent) {
        evt.voiceStatus.let {
            when (it) {
                VoiceStatus.BOTH_CONNECTED_DIFFERENT_CHANNEL -> {
                    evt.channel.sendMessage("I'm already in a voice channel.").complete()
                }
                VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> {
                    connectToVoiceChannel(evt.member.voiceState.channel)
                    evt.channel.sendMessage("Now connected to ${evt.member.voiceState.channel.name}.").complete()
                }
                else -> {
                    it.sendErrorMessage(evt.channel)
                }
            }
        }
    }

    @Command(description = "Leaves the voice channel that the bot is in.", locale = Locale.GUILD)
    fun leaveVoice(evt: MessageReceivedEvent) {
        leaveVoiceChannel(evt.guild)
        evt.channel.sendMessage("Left ${evt.guild.audioManager.connectedChannel.name}.").complete()
    }

    @Command(
        description = "Plays a URL, or searches YouTube for the given search terms.",
        locale = Locale.GUILD,
        splitLastParameter = false
    )
    fun play(evt: MessageReceivedEvent, query: String) {
        handleVoiceStatus(evt, true)
        val formattedQuery = StringBuilder().apply {
            if (urlValidator.isValid(query)) {
                append("ytsearch:")
            }
            append(query)
        }.toString()
        playerManager.loadItemOrdered(evt.guild.musicManager, formattedQuery, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                evt.channel.sendMessage("Adding ${track.info.title} to queue.").complete()
                evt.guild.musicManager.addToQueue(track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                if (playlist.isSearchResult) {
                    val track = playlist.tracks[0]
                    evt.guild.musicManager.addToQueue(track)
                    evt.channel.sendMessage("Adding ${track.info.title} to queue.").complete()
                } else {
                    evt.channel.sendMessage("Adding ${playlist.tracks.size} songs from ${playlist.name} to queue.")
                        .complete()
                    playlist.tracks.forEach { evt.guild.musicManager.addToQueue(it) }
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

    @Command(description = "Skips the currently playing song.", locale = Locale.GUILD)
    fun skip(evt: MessageReceivedEvent) {
        evt.voiceStatus.let {
            if (it != VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL) {
                it.sendErrorMessage(evt.channel)
                return
            }
        }
        if (evt.guild.musicManager.queue.isEmpty() && evt.guild.musicManager.playingTrack != null) {
            evt.channel.sendMessage("Cannot skip. Nothing is playing.").complete()
            return
        }
        evt.guild.musicManager.skipTrack()
        evt.channel.sendMessage("Skipped to next track.").complete()
    }

    @Command(
        description = "Stops playing music and clears the queue.",
        locale = Locale.GUILD,
        memberPermissions = [Permission.VOICE_MUTE_OTHERS]
    )
    fun stop(evt: MessageReceivedEvent) {
        evt.guild.musicManager.stop()
        evt.channel.sendMessage("Cleared the music queue.").complete()
    }

    @Command(description = "Pauses the currently playing song.", locale = Locale.GUILD)
    fun pause(evt: MessageReceivedEvent) {
        evt.voiceStatus.let {
            if (it != VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL) {
                it.sendErrorMessage(evt.channel)
                return
            }
        }
        evt.channel.sendMessage("Paused.").complete()
    }

    @Command(description = "Resumes the currently playing song.", locale = Locale.GUILD)
    fun unPause(evt: MessageReceivedEvent) {
        evt.voiceStatus.let {
            if (it != VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL) {
                it.sendErrorMessage(evt.channel)
                return
            }
        }
        evt.channel.sendMessage("Resumed.").complete()
    }

    @Command(description = "Sends an embed with the list of songs in the queue.", locale = Locale.GUILD)
    fun queue(evt: MessageReceivedEvent) {
        evt.guild.musicManager.playingTrack?.let { playingTrack ->
            evt.channel.sendEmbed {
                val position = (playingTrack.position / millisecondsPerSecond).toBasicTimestamp()
                val duration = (playingTrack.duration / millisecondsPerSecond).toBasicTimestamp()
                val upNextList = evt.guild.musicManager.queue.take(queueListLength).joinToString("\n") {
                    "${it.info.title} (${(it.duration / millisecondsPerSecond).toBasicTimestamp()})"
                }
                addField(
                    "Now Playing",
                    "${playingTrack.info.title} ($position/$duration)",
                    false
                )
                if (evt.guild.musicManager.queue.isNotEmpty()) addField(
                    "Up Next",
                    upNextList + if (evt.guild.musicManager.queue.size > queueListLength) {
                        "\n plus ${evt.guild.musicManager.queue.size - queueListLength} more..."
                    } else "",
                    false
                )
            }.complete()
        } ?: evt.channel.sendMessage("No songs are queued.").complete()
    }

    @Command(description = "Sets the volume.", locale = Locale.GUILD)
    fun setVolume(evt: MessageReceivedEvent, volume: Int) {
        if (handleVoiceStatus(evt)) {
            evt.guild.musicManager.volume = volume
            evt.channel.sendMessage("Set volume to ${evt.guild.musicManager.volume}%.").complete()
        }
    }

    @Listener
    fun leaveVoiceAutomatically(evt: GuildVoiceLeaveEvent) {
        if (evt.guild.audioManager.connectedChannel != evt.channelLeft) return
        if (evt.guild.audioManager.connectedChannel.members.all { it.user.isBot }) {
            leaveVoiceChannel(evt.guild)
        }
    }

    @Listener
    fun leaveVoiceAutomatically(evt: GuildVoiceMoveEvent) {
        if (evt.guild.audioManager.connectedChannel != evt.channelLeft) return
        if (evt.guild.audioManager.connectedChannel.members.all { it.user.isBot }) {
            leaveVoiceChannel(evt.guild)
        }
    }

    private fun leaveVoiceChannel(guild: Guild) {
        if (guild.audioManager.isConnected) {
            guild.musicManager.reset()
            guild.audioManager.closeAudioConnection()
        }
    }

    private fun connectToVoiceChannel(voiceChannel: VoiceChannel) {
        val audioManager = voiceChannel.guild.audioManager
        if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
            audioManager.openAudioConnection(voiceChannel)
        }
    }

    private fun handleVoiceStatus(evt: MessageReceivedEvent, shouldConnect: Boolean = false): Boolean =
        evt.voiceStatus.let {
            when (it) {
                VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> true
                VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> {
                    if (shouldConnect) {
                        connectToVoiceChannel(evt.member.voiceState.channel)
                        true
                    } else {
                        it.sendErrorMessage(evt.channel)
                        false
                    }
                }
                else -> {
                    it.sendErrorMessage(evt.channel)
                    false
                }
            }
        }

    private val MessageReceivedEvent.voiceStatus: VoiceStatus
        get() {
            val selfIsConnected = guild.audioManager.isConnected
            val userIsConnected = member.voiceState.inVoiceChannel()
            val differentChannel = member.voiceState.channel != guild.audioManager.connectedChannel
            return when {
                !userIsConnected && selfIsConnected -> VoiceStatus.SELF_CONNECTED_USER_DISCONNECTED
                !selfIsConnected && userIsConnected -> VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED
                differentChannel -> VoiceStatus.BOTH_CONNECTED_DIFFERENT_CHANNEL
                else -> VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL
            }
        }

    private enum class VoiceStatus(val errorMessage: String?) {
        NEITHER_CONNECTED("We both need to be in a voice channel for me to do that."),
        SELF_DISCONNECTED_USER_CONNECTED("I need to be in your voice channel to do that."),
        SELF_CONNECTED_USER_DISCONNECTED("You need to be in a voice channel for me to do that."),
        BOTH_CONNECTED_DIFFERENT_CHANNEL("We need to be in the same voice channel for you to do that."),
        BOTH_CONNECTED_SAME_CHANNEL(null);

        fun sendErrorMessage(channel: MessageChannel) {
            errorMessage?.let {
                channel.sendMessage(it).complete()
            }
        }
    }

    private class GuildMusicManager(manager: AudioPlayerManager) : AudioEventAdapter() {
        private val player: AudioPlayer = manager.createPlayer().also {
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

    companion object {
        private const val queueListLength = 8
        private const val millisecondsPerSecond = 1000
    }
}
