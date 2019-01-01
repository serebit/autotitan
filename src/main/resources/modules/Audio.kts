import com.serebit.autotitan.api.extensions.jda.closeAudioConnection
import com.serebit.autotitan.api.extensions.jda.openAudioConnection
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.audio.AudioHandler
import com.serebit.autotitan.audio.GuildTrackManager
import com.serebit.autotitan.audio.VoiceStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

val trackManagers = mutableMapOf<Long, GuildTrackManager>()
val uriRegex = "^https?://[^\\s/\$.?#].[^\\s]*\$".toRegex()

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
            VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> connectToVoiceChannel(this.member.voiceState.channel) {
                this.channel.sendMessage("Joined ${this.guild.audioManager.connectedChannel.name}.").queue()
            }
            VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> {
                this.channel.sendMessage("We're already in the same voice channel.").queue()
            }
            VoiceStatus.BOTH_CONNECTED_DIFFERENT_CHANNEL, VoiceStatus.SELF_CONNECTED_USER_DISCONNECTED -> {
                this.channel.sendMessage("I'm already in a voice channel.").queue()
            }
            VoiceStatus.NEITHER_CONNECTED -> {
                this.channel.sendMessage("You need to be in a voice channel for me to do that.").queue()
            }
        }
    }

    command("leaveVoice", "Leaves the voice channel that the bot is in.") {
        leaveVoiceChannel(this.guild) {
            this.channel.sendMessage("Left ${this.guild.audioManager.connectedChannel.name}.").queue()
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
            AudioHandler.loadTrack(formattedQuery, this.textChannel) { track ->
                this.channel.sendMessage("Adding ${track.info.title} to queue.").queue()
                this.guild.trackManager.addToQueue(track)
            }
        }
    }

    command("playPlaylist", "Plays a playlist from the given URI.") { uri: String ->
        if (handleVoiceStatus(this, true)) {
            val trimmedUri = uri.removeSurrounding("<", ">")
            if (trimmedUri.matches(uriRegex)) {
                AudioHandler.loadPlaylist(trimmedUri, this.textChannel) { playlist ->
                    this.channel.sendMessage(
                        "Adding ${playlist.tracks.size} tracks from ${playlist.name} to queue."
                    ).queue()
                    playlist.tracks.forEach(this.guild.trackManager::addToQueue)
                }
            } else {
                this.channel.sendMessage("That link is invalid.").queue()
            }
        }
    }

    command("skip", "Skips the currently playing song.") {
        if (handleVoiceStatus(this)) {
            if (this.guild.trackManager.isPlaying) {
                this.guild.trackManager.skipTrack()
                this.channel.sendMessage("Skipped.").queue()
            } else this.channel.sendMessage("Can't skip. Nothing is playing.").queue()
        }
    }

    command(
        "stop",
        "Stops playing music and clears the queue. Can only be used by members above the bot's role.",
        Access.Guild.RankAbove()
    ) {
        this.guild.trackManager.stop()
        this.channel.sendMessage("Cleared the music queue.").queue()
    }

    command("pause", "Pauses the currently playing song.") {
        if (handleVoiceStatus(this) && this.guild.trackManager.isNotPaused) {
            this.guild.trackManager.pause()
            this.channel.sendMessage("Paused the track.").queue()
        } else this.channel.sendMessage("The track is already paused.").queue()
    }

    command("unPause", "Resumes the currently playing song.") {
        if (handleVoiceStatus(this) && this.guild.trackManager.isPaused) {
            this.guild.trackManager.resume()
            this.channel.sendMessage("Unpaused the track.").queue()
        } else this.channel.sendMessage("The track is already playing.").queue()
    }

    command(
        "queue",
        "Sends an embed with the list of songs in the queue."
    ) { this.guild.trackManager.sendQueueEmbed(this.textChannel) }

    command("setVolume", "Sets the volume.") { volume: Int ->
        if (handleVoiceStatus(this)) {
            this.guild.trackManager.volume = volume
            this.channel.sendMessage("Set the volume to ${this.guild.trackManager.volume}%.").queue()
        }
    }

    listener<GuildVoiceMoveEvent> {
        if (this.guild.audioManager.connectedChannel != this.channelLeft) return@listener
        if (this.guild.audioManager.connectedChannel.members.all { it.user.isBot }) {
            leaveVoiceChannel(this.guild)
        }
    }

    listener<GuildVoiceLeaveEvent> {
        if (this.guild.audioManager.connectedChannel != this.channelLeft) return@listener
        if (this.guild.audioManager.connectedChannel.members.all { it.user.isBot }) {
            leaveVoiceChannel(this.guild)
        }
    }
}
