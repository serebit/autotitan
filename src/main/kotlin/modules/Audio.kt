package com.serebit.autotitan.modules

import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.extensions.jda.closeAudioConnection
import com.serebit.autotitan.api.extensions.jda.openAudioConnection
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.audio.AudioHandler
import com.serebit.autotitan.audio.GuildTrackManager
import com.serebit.autotitan.audio.VoiceStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@Suppress("UNUSED")
class Audio : ModuleTemplate(defaultAccess = Access.Guild.All()) {
    private val uriRegex = "^https?://[^\\s/\$.?#].[^\\s]*\$".toRegex()

    init {
        command("joinVoice", "Joins the voice channel that the invoker is in.") { evt ->
            when (VoiceStatus.from(evt)) {
                VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> connectToVoiceChannel(evt.member.voiceState.channel) {
                    evt.channel.sendMessage("Joined ${evt.guild.audioManager.connectedChannel.name}.").queue()
                }
                VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> {
                    evt.channel.sendMessage("We're already in the same voice channel.").queue()
                }
                VoiceStatus.BOTH_CONNECTED_DIFFERENT_CHANNEL, VoiceStatus.SELF_CONNECTED_USER_DISCONNECTED -> {
                    evt.channel.sendMessage("I'm already in a voice channel.").queue()
                }
                VoiceStatus.NEITHER_CONNECTED -> {
                    evt.channel.sendMessage("You need to be in a voice channel for me to do that.").queue()
                }
            }
        }

        command("leaveVoice", "Leaves the voice channel that the bot is in.") { evt ->
            leaveVoiceChannel(evt.guild) {
                evt.channel.sendMessage("Left ${evt.guild.audioManager.connectedChannel.name}.").queue()
            }
        }

        command(
            "play",
            "Plays a track from a URI, or searches YouTube for the given search terms."
        ) { evt, query: LongString ->
            if (handleVoiceStatus(evt, true)) {
                val trimmedQuery = query.value.removeSurrounding("<", ">")
                val formattedQuery = buildString {
                    if (!trimmedQuery.matches(uriRegex)) append("ytsearch: ")
                    append(trimmedQuery)
                }
                AudioHandler.loadTrack(formattedQuery, evt.textChannel) { track ->
                    evt.channel.sendMessage("Adding ${track.info.title} to queue.").queue()
                    evt.guild.trackManager.addToQueue(track)
                }
            }
        }

        command("playPlaylist", "Plays a playlist from the given URI.") { evt, uri: String ->
            if (handleVoiceStatus(evt, true)) {
                val trimmedUri = uri.removeSurrounding("<", ">")
                if (trimmedUri.matches(uriRegex)) {
                    AudioHandler.loadPlaylist(trimmedUri, evt.textChannel) { playlist ->
                        evt.channel.sendMessage(
                            "Adding ${playlist.tracks.size} tracks from ${playlist.name} to queue."
                        ).queue()
                        playlist.tracks.forEach(evt.guild.trackManager::addToQueue)
                    }
                } else {
                    evt.channel.sendMessage("That link is invalid.").queue()
                }
            }
        }

        command("skip", "Skips the currently playing song.") { evt ->
            if (handleVoiceStatus(evt)) {
                if (evt.guild.trackManager.isPlaying) {
                    evt.guild.trackManager.skipTrack()
                    evt.channel.sendMessage("Skipped.").queue()
                } else evt.channel.sendMessage("Can't skip. Nothing is playing.").queue()
            }
        }

        command(
            "stop",
            "Stops playing music and clears the queue. Can only be used by members above the bot's role.",
            Access.Guild.RankAbove()
        ) { evt ->
            evt.guild.trackManager.stop()
            evt.channel.sendMessage("Cleared the music queue.").queue()
        }

        command("pause", "Pauses the currently playing song.") { evt ->
            if (handleVoiceStatus(evt) && evt.guild.trackManager.isNotPaused) {
                evt.guild.trackManager.pause()
                evt.channel.sendMessage("Paused the track.").queue()
            } else evt.channel.sendMessage("The track is already paused.").queue()
        }

        command("unPause", "Resumes the currently playing song.") { evt ->
            if (handleVoiceStatus(evt) && evt.guild.trackManager.isPaused) {
                evt.guild.trackManager.resume()
                evt.channel.sendMessage("Unpaused the track.").queue()
            } else evt.channel.sendMessage("The track is already playing.").queue()
        }

        command(
            "queue",
            "Sends an embed with the list of songs in the queue."
        ) { evt -> evt.guild.trackManager.sendQueueEmbed(evt.textChannel) }

        command("setVolume", "Sets the volume.") { evt, volume: Int ->
            if (handleVoiceStatus(evt)) {
                evt.guild.trackManager.volume = volume
                evt.channel.sendMessage("Set the volume to ${evt.guild.trackManager.volume}%.").queue()
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

    private fun handleVoiceStatus(evt: MessageReceivedEvent, shouldConnect: Boolean = false): Boolean =
        VoiceStatus.from(evt).let { status ->
            when (status) {
                VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> true
                VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> if (shouldConnect) {
                    connectToVoiceChannel(evt.member.voiceState.channel)
                    true
                } else {
                    status.sendErrorMessage(evt.textChannel)
                    false
                }
                else -> {
                    status.sendErrorMessage(evt.textChannel)
                    false
                }
            }
        }

    companion object {
        private val trackManagers = mutableMapOf<Long, GuildTrackManager>()
        private const val queueListLength = 8
        private const val millisecondsPerSecond = 1000

        val Guild.trackManager: GuildTrackManager
            get() = trackManagers.getOrPut(idLong) {
                GuildTrackManager(audioManager).also {
                    audioManager.sendingHandler = it.sendHandler
                }
            }
    }
}
