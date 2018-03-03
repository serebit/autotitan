package com.serebit.autotitan.modules

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.api.meta.annotations.Listener
import com.serebit.autotitan.audio.AudioHandler
import com.serebit.autotitan.audio.VoiceStatus
import com.serebit.extensions.jda.musicManager
import com.serebit.extensions.jda.openAudioConnection
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.jda.voiceStatus
import com.serebit.extensions.toBasicTimestamp
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.apache.commons.validator.routines.UrlValidator

@Suppress("UNUSED", "TooManyFunctions")
class Audio : Module(isOptional = true) {
    private val urlValidator = UrlValidator(arrayOf("http", "https"))
    private val playerManager = DefaultAudioPlayerManager()

    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    @Command(description = "Joins the voice channel that the invoker is in.", locale = Locale.GUILD)
    fun joinVoice(evt: MessageReceivedEvent) {
        val voiceStatus = evt.voiceStatus
        when (voiceStatus) {
            VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> {
                connectToVoiceChannel(evt.channel, evt.member.voiceState.channel)
            }
            VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> {
                evt.channel.sendMessage("We're already in the same voice channel.").complete()
            }
            VoiceStatus.BOTH_CONNECTED_DIFFERENT_CHANNEL, VoiceStatus.SELF_CONNECTED_USER_DISCONNECTED -> {
                evt.channel.sendMessage("I'm already in a voice channel.").complete()
            }
            VoiceStatus.NEITHER_CONNECTED -> {
                evt.channel.sendMessage("You need to be in a voice channel for me to do that.").complete()
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
        if (handleVoiceStatus(evt, true)) {
            val formattedQuery = buildString {
                if (!urlValidator.isValid(query)) {
                    append("ytsearch:")
                }
                append(query)
            }
            AudioHandler.loadTrack(formattedQuery, evt.textChannel) { track ->
                evt.channel.sendMessage("Adding ${track.info.title} to queue.").complete()
                evt.guild.musicManager.addToQueue(track)
            }
        }
    }

    @Command(description = "Skips the currently playing song.", locale = Locale.GUILD)
    fun skip(evt: MessageReceivedEvent) {
        if (handleVoiceStatus(evt)) {
            if (evt.guild.musicManager.queue.isEmpty() || evt.guild.musicManager.playingTrack != null) {
                evt.channel.sendMessage("Cannot skip. Nothing is playing.").complete()
            } else {
                evt.guild.musicManager.skipTrack()
                evt.channel.sendMessage("Skipped to next track.").complete()
            }
        }
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
        if (handleVoiceStatus(evt) && evt.guild.musicManager.playingTrack != null) {
            evt.guild.musicManager.pause()
            evt.channel.sendMessage("Paused the track.").complete()
        }
    }

    @Command(description = "Resumes the currently playing song.", locale = Locale.GUILD)
    fun unPause(evt: MessageReceivedEvent) {
        if (handleVoiceStatus(evt)) {
            evt.channel.sendMessage("Unpaused.").complete()
        }
    }

    @Command(description = "Sends an embed with the list of songs in the queue.", locale = Locale.GUILD)
    fun queue(evt: MessageReceivedEvent) {
        evt.guild.musicManager.playingTrack?.let { playingTrack ->
            evt.channel.sendEmbed {
                val position = (playingTrack.position / millisecondsPerSecond).toBasicTimestamp()
                val duration = (playingTrack.duration / millisecondsPerSecond).toBasicTimestamp()
                val upNextList = evt.guild.musicManager.queue
                    .take(queueListLength)
                    .joinToString("\n") {
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

    private fun connectToVoiceChannel(textChannel: MessageChannel, voiceChannel: VoiceChannel) {
        val audioManager = voiceChannel.guild.audioManager
        if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
            audioManager.openAudioConnection(voiceChannel) {
                textChannel.sendMessage("Joined ${audioManager.connectedChannel.name}.").complete()
            }
        }
    }

    private fun handleVoiceStatus(
        evt: MessageReceivedEvent,
        shouldConnect: Boolean = false
    ): Boolean {
        val voiceStatus = evt.voiceStatus
        return when (evt.voiceStatus) {
            VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> true
            VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> {
                if (shouldConnect) {
                    connectToVoiceChannel(evt.textChannel, evt.member.voiceState.channel)
                    true
                } else {
                    voiceStatus.sendErrorMessage(evt.textChannel)
                    false
                }
            }
            else -> {
                voiceStatus.sendErrorMessage(evt.textChannel)
                false
            }
        }
    }

    companion object {
        private const val queueListLength = 8
        private const val millisecondsPerSecond = 1000
    }
}
