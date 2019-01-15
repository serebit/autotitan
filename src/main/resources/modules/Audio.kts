import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.audio.AudioHandler
import com.serebit.autotitan.audio.GuildTrackManager
import com.serebit.autotitan.audio.VoiceStatus
import net.dv8tion.jda.core.audio.hooks.ConnectionListener
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.managers.AudioManager

val trackManagers = mutableMapOf<Long, GuildTrackManager>()
val uriRegex = "^https?://[^\\s/\$.?#].[^\\s]*\$".toRegex()

inline fun AudioManager.onConnectionStatusChange(desiredStatus: ConnectionStatus, crossinline task: () -> Unit) {
    connectionListener = object : ConnectionListener {
        override fun onStatusChange(status: ConnectionStatus) {
            if (status == desiredStatus) {
                task()
                connectionListener = null
            }
        }

        override fun onUserSpeaking(user: User?, speaking: Boolean) = Unit

        override fun onPing(ping: Long) = Unit
    }
}

inline fun AudioManager.openAudioConnection(channel: VoiceChannel, crossinline task: () -> Unit) {
    onConnectionStatusChange(ConnectionStatus.CONNECTED, task)
    openAudioConnection(channel)
}

inline fun AudioManager.closeAudioConnection(crossinline task: () -> Unit) {
    onConnectionStatusChange(ConnectionStatus.NOT_CONNECTED, task)
    closeAudioConnection()
}

inline fun leaveVoiceChannel(guild: Guild, crossinline onDisconnect: () -> Unit = {}) {
    if (guild.audioManager.isConnected) {
        guild.trackManager.reset()
        guild.audioManager.closeAudioConnection(onDisconnect)
    }
}

inline fun connectToVoiceChannel(voiceChannel: VoiceChannel, crossinline onConnect: () -> Unit = {}) {
    val audioManager = voiceChannel.guild.audioManager
    if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
        audioManager.openAudioConnection(voiceChannel, onConnect)
    }
}

fun handleVoiceStatus(evt: MessageReceivedEvent, shouldConnect: Boolean = false): Boolean =
    when (val status = VoiceStatus.from(evt)) {
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

val Guild.trackManager: GuildTrackManager
    get() = trackManagers.getOrPut(idLong) {
        GuildTrackManager(audioManager).also {
            audioManager.sendingHandler = it.sendHandler
        }
    }

module("Audio", defaultAccess = Access.Guild.All()) {
    command("joinVoice", "Joins the voice channel that the invoker is in.") {
        when (VoiceStatus.from(this)) {
            VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> connectToVoiceChannel(member.voiceState.channel) {
                channel.sendMessage("Joined ${guild.audioManager.connectedChannel.name}.").queue()
            }
            VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> {
                channel.sendMessage("We're already in the same voice channel.").queue()
            }
            VoiceStatus.BOTH_CONNECTED_DIFFERENT_CHANNEL, VoiceStatus.SELF_CONNECTED_USER_DISCONNECTED -> {
                channel.sendMessage("I'm already in a voice channel.").queue()
            }
            VoiceStatus.NEITHER_CONNECTED -> {
                channel.sendMessage("You need to be in a voice channel for me to do that.").queue()
            }
        }
    }

    command("leaveVoice", "Leaves the voice channel that the bot is in.") {
        leaveVoiceChannel(guild) {
            channel.sendMessage("Left ${guild.audioManager.connectedChannel.name}.").queue()
        }
    }

    command(
        "play",
        "Plays a track from a URI, or searches YouTube for the given search terms."
    ) { query: LongString ->
        if (handleVoiceStatus(this, true)) {
            val trimmedQuery = query.value.removeSurrounding("<", ">")
            val formattedQuery = buildString {
                if (!trimmedQuery.matches(uriRegex)) append("ytsearch: ")
                append(trimmedQuery)
            }
            AudioHandler.loadTrack(formattedQuery, textChannel) { track ->
                channel.sendMessage("Adding ${track.info.title} to queue.").queue()
                guild.trackManager.addToQueue(track)
            }
        }
    }

    command("playPlaylist", "Plays a playlist from the given URI.") { uri: String ->
        if (handleVoiceStatus(this, true)) {
            val trimmedUri = uri.removeSurrounding("<", ">")
            if (trimmedUri.matches(uriRegex)) {
                AudioHandler.loadPlaylist(trimmedUri, textChannel) { playlist ->
                    channel.sendMessage(
                        "Adding ${playlist.tracks.size} tracks from ${playlist.name} to queue."
                    ).queue()
                    playlist.tracks.forEach(guild.trackManager::addToQueue)
                }
            } else {
                channel.sendMessage("That link is invalid.").queue()
            }
        }
    }

    command("skip", "Skips the currently playing song.") {
        if (handleVoiceStatus(this)) {
            if (guild.trackManager.isPlaying) {
                guild.trackManager.skipTrack()
                channel.sendMessage("Skipped.").queue()
            } else channel.sendMessage("Can't skip. Nothing is playing.").queue()
        }
    }

    command(
        "stop",
        "Stops playing music and clears the queue. Can only be used by members above the bot's role.",
        Access.Guild.RankAbove()
    ) {
        guild.trackManager.stop()
        channel.sendMessage("Cleared the music queue.").queue()
    }

    command("pause", "Pauses the currently playing song.") {
        if (handleVoiceStatus(this) && guild.trackManager.isNotPaused) {
            guild.trackManager.pause()
            channel.sendMessage("Paused the track.").queue()
        } else channel.sendMessage("The track is already paused.").queue()
    }

    command("unPause", "Resumes the currently playing song.") {
        if (handleVoiceStatus(this) && guild.trackManager.isPaused) {
            guild.trackManager.resume()
            channel.sendMessage("Unpaused the track.").queue()
        } else channel.sendMessage("The track is already playing.").queue()
    }

    command(
        "queue",
        "Sends an embed with the list of songs in the queue."
    ) { guild.trackManager.sendQueueEmbed(textChannel) }

    command("setVolume", "Sets the volume.") { volume: Int ->
        if (handleVoiceStatus(this)) {
            guild.trackManager.volume = volume
            channel.sendMessage("Set the volume to ${guild.trackManager.volume}%.").queue()
        }
    }

    listener<GuildVoiceMoveEvent> {
        if (guild.audioManager.connectedChannel != channelLeft) return@listener
        if (guild.audioManager.connectedChannel.members.all { it.user.isBot }) {
            leaveVoiceChannel(guild)
        }
    }

    listener<GuildVoiceLeaveEvent> {
        if (guild.audioManager.connectedChannel != channelLeft) return@listener
        if (guild.audioManager.connectedChannel.members.all { it.user.isBot }) {
            leaveVoiceChannel(guild)
        }
    }
}
