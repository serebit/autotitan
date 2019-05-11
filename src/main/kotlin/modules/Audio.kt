package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.api.annotations.Listener
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.audio.AudioHandler
import com.serebit.autotitan.audio.VoiceStatus
import com.serebit.autotitan.extensions.jda.closeAudioConnection
import com.serebit.autotitan.extensions.jda.openAudioConnection
import com.serebit.autotitan.extensions.jda.trackManager
import com.serebit.autotitan.extensions.jda.voiceStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.apache.commons.validator.routines.UrlValidator

@Suppress("UNUSED", "TooManyFunctions")
class Audio : Module() {
    private val urlValidator = UrlValidator(arrayOf("http", "https"))

    @Command(description = "Joins the voice channel that the invoker is in.", locale = Locale.GUILD)
    fun joinVoice(evt: MessageReceivedEvent) {
        val voiceStatus = evt.voiceStatus
        when (voiceStatus) {
            VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> connectToVoiceChannel(evt.member.voiceState.channel) {
                evt.channel.sendMessage("Joined ${evt.guild.audioManager.connectedChannel.name}.").complete()
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
        val channelName = evt.guild.audioManager.connectedChannel.name
        leaveVoiceChannel(evt.guild) {
            evt.channel.sendMessage("Left $channelName.").complete()
        }
    }

    @Command(
        description = "Plays a track from a URI, or searches YouTube for the given search terms.",
        locale = Locale.GUILD,
        splitLastParameter = false
    )
    fun play(evt: MessageReceivedEvent, query: String) {
        if (handleVoiceStatus(evt, true)) {
            val trimmedQuery = query.removeSurrounding("<", ">")
            val formattedQuery = buildString {
                if (!urlValidator.isValid(trimmedQuery)) {
                    append("ytsearch: ")
                }
                append(trimmedQuery)
            }
            AudioHandler.loadTrack(formattedQuery, evt.textChannel) { track ->
                evt.channel.sendMessage("Adding ${track.info.title} to queue.").complete()
                evt.guild.trackManager.addToQueue(track)
            }
        }
    }

    @Command(description = "Plays a playlist from the given URI.", locale = Locale.GUILD)
    fun playPlaylist(evt: MessageReceivedEvent, uri: String) {
        if (handleVoiceStatus(evt, true)) {
            val trimmedUri = uri.removeSurrounding("<", ">")
            if (urlValidator.isValid(trimmedUri)) {
                AudioHandler.loadPlaylist(trimmedUri, evt.textChannel) { playlist ->
                    evt.channel.sendMessage(
                        "Adding ${playlist.tracks.size} tracks from ${playlist.name} to queue."
                    ).complete()
                    playlist.tracks.forEach(evt.guild.trackManager::addToQueue)
                }
            } else {
                evt.channel.sendMessage("That URI isn't a valid playlist.").complete()
            }
        }
    }

    @Command(description = "Skips the currently playing song.", locale = Locale.GUILD)
    fun skip(evt: MessageReceivedEvent) {
        if (handleVoiceStatus(evt)) {
            if (evt.guild.trackManager.isPlaying) {
                evt.guild.trackManager.skipTrack()
                evt.channel.sendMessage("Skipped.").complete()
            } else {
                evt.channel.sendMessage("Cannot skip. Nothing is playing.").complete()
            }
        }
    }

    @Command(
        description = "Stops playing music and clears the queue. Can only be used by members above the bot's role.",
        locale = Locale.GUILD,
        access = Access.RANK_ABOVE
    )
    fun stop(evt: MessageReceivedEvent) {
        evt.guild.trackManager.stop()
        evt.channel.sendMessage("Cleared the music queue.").complete()
    }

    @Command(description = "Pauses the currently playing song.", locale = Locale.GUILD)
    fun pause(evt: MessageReceivedEvent) {
        if (handleVoiceStatus(evt) && evt.guild.trackManager.isNotPaused) {
            evt.guild.trackManager.pause()
            evt.channel.sendMessage("Paused the track.").complete()
        } else evt.channel.sendMessage("The track is already paused.").complete()
    }

    @Command(description = "Resumes the currently playing song.", locale = Locale.GUILD)
    fun unPause(evt: MessageReceivedEvent) {
        if (handleVoiceStatus(evt) && evt.guild.trackManager.isPaused) {
            evt.guild.trackManager.resume()
            evt.channel.sendMessage("Unpaused the track.").complete()
        } else evt.channel.sendMessage("The track is already playing.").complete()
    }

    @Command(description = "Sends an embed with the list of songs in the queue.", locale = Locale.GUILD)
    fun queue(evt: MessageReceivedEvent) {
        evt.guild.trackManager.sendQueueEmbed(evt.textChannel)
    }

    @Command(description = "Sets the volume.", locale = Locale.GUILD)
    fun setVolume(evt: MessageReceivedEvent, volume: Int) {
        if (handleVoiceStatus(evt)) {
            evt.guild.trackManager.volume = volume
            evt.channel.sendMessage("Set volume to ${evt.guild.trackManager.volume}%.").complete()
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

    private inline fun leaveVoiceChannel(guild: Guild, crossinline onDisconnect: () -> Unit = {}) {
        if (guild.audioManager.isConnected) {
            guild.trackManager.reset()
            guild.audioManager.closeAudioConnection(onDisconnect)
        }
    }

    private inline fun connectToVoiceChannel(voiceChannel: VoiceChannel, crossinline onConnect: () -> Unit = {}) {
        val audioManager = voiceChannel.guild.audioManager
        if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
            audioManager.openAudioConnection(voiceChannel, onConnect)
        }
    }

    private fun handleVoiceStatus(evt: MessageReceivedEvent, shouldConnect: Boolean = false): Boolean {
        val voiceStatus = evt.voiceStatus
        return when (evt.voiceStatus) {
            VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> true
            VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> if (shouldConnect) {
                connectToVoiceChannel(evt.member.voiceState.channel)
                true
            } else {
                voiceStatus.sendErrorMessage(evt.textChannel)
                false
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
