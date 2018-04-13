package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Restrictions
import com.serebit.autotitan.audio.AudioHandler
import com.serebit.autotitan.audio.VoiceStatus
import com.serebit.extensions.jda.closeAudioConnection
import com.serebit.extensions.jda.openAudioConnection
import com.serebit.extensions.jda.trackManager
import com.serebit.extensions.jda.voiceStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.apache.commons.validator.routines.UrlValidator

@Suppress("UNUSED", "TooManyFunctions")
class Audio : Module() {
    private val urlValidator = UrlValidator(arrayOf("http", "https"))

    init {
        command(
            "joinVoice",
            "Joins the voice channel that the invoker is in.",
            Restrictions(Access.GUILD_ALL)
        ) { evt: MessageReceivedEvent ->
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

        command(
            "leaveVoice",
            "Leaves the voice channel that the bot is in.",
            Restrictions(Access.GUILD_ALL)
        ) { evt: MessageReceivedEvent ->
            val channelName = evt.guild.audioManager.connectedChannel.name
            leaveVoiceChannel(evt.guild) {
                evt.channel.sendMessage("Left $channelName.").complete()
            }
        }

        command(
            "play",
            "Plays a track from a URI, or searches YouTube for the given search terms.",
            Restrictions(Access.GUILD_ALL),
            delimitLastString = false
        ) { evt: MessageReceivedEvent, query: String ->
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

        command(
            "playPlaylist",
            "Plays a playlist from the given URI.",
            Restrictions(Access.GUILD_ALL)
        ) { evt: MessageReceivedEvent, uri: String ->
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

        command(
            "skip",
            "Skips the currently playing song.",
            Restrictions(Access.GUILD_ALL)
        ) { evt: MessageReceivedEvent ->
            if (handleVoiceStatus(evt)) {
                if (evt.guild.trackManager.isPlaying) {
                    evt.guild.trackManager.skipTrack()
                    evt.channel.sendMessage("Skipped.").complete()
                } else {
                    evt.channel.sendMessage("Cannot skip. Nothing is playing.").complete()
                }
            }
        }

        command(
            "stop",
            "Stops playing music and clears the queue. Can only be used by members above the bot's role.",
            Restrictions(Access.GUILD_RANK_ABOVE)
        ) { evt: MessageReceivedEvent ->
            evt.guild.trackManager.stop()
            evt.channel.sendMessage("Cleared the music queue.").complete()
        }

        command(
            "pause",
            "Pauses the currently playing song.",
            Restrictions(Access.GUILD_ALL)
        ) { evt: MessageReceivedEvent ->
            if (handleVoiceStatus(evt) && evt.guild.trackManager.isNotPaused) {
                evt.guild.trackManager.pause()
                evt.channel.sendMessage("Paused the track.").complete()
            } else evt.channel.sendMessage("The track is already paused.").complete()
        }

        command(
            "unPause",
            "Resumes the currently playing song.",
            Restrictions(Access.GUILD_ALL)
        ) { evt: MessageReceivedEvent ->
            if (handleVoiceStatus(evt) && evt.guild.trackManager.isPaused) {
                evt.guild.trackManager.resume()
                evt.channel.sendMessage("Unpaused the track.").complete()
            } else evt.channel.sendMessage("The track is already playing.").complete()
        }

        command(
            "queue",
            "Sends an embed with the list of songs in the queue.",
            Restrictions(Access.GUILD_ALL)
        ) { evt: MessageReceivedEvent ->
            evt.guild.trackManager.sendQueueEmbed(evt.textChannel)
        }

        command(
            "setVolume",
            "Sets the volume.",
            Restrictions(Access.GUILD_ALL)
        ) { evt: MessageReceivedEvent, volume: Int ->
            if (handleVoiceStatus(evt)) {
                evt.guild.trackManager.volume = volume
                evt.channel.sendMessage("Set volume to ${evt.guild.trackManager.volume}%.").complete()
            }
        }

        listener { evt: GuildVoiceLeaveEvent ->
            if (evt.guild.audioManager.connectedChannel != evt.channelLeft) return@listener
            if (evt.guild.audioManager.connectedChannel.members.all { it.user.isBot }) {
                leaveVoiceChannel(evt.guild)
            }
        }

        listener { evt: GuildVoiceMoveEvent ->
            if (evt.guild.audioManager.connectedChannel != evt.channelLeft) return@listener
            if (evt.guild.audioManager.connectedChannel.members.all { it.user.isBot }) {
                leaveVoiceChannel(evt.guild)
            }
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
