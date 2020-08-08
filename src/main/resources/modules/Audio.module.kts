import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState
import com.serebit.autotitan.api.*
import com.serebit.autotitan.extensions.sendEmbed
import com.serebit.logkat.error
import com.serebit.logkat.warn
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.audio.hooks.ConnectionListener
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.managers.AudioManager
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.Future

enum class VoiceStatus(private val errorMessage: String?) {
    NEITHER_CONNECTED("We both need to be in a voice channel for me to do that."),
    SELF_DISCONNECTED_USER_CONNECTED("I need to be in your voice channel to do that."),
    SELF_CONNECTED_USER_DISCONNECTED("You need to be in a voice channel for me to do that."),
    BOTH_CONNECTED_DIFFERENT_CHANNEL("We need to be in the same voice channel for you to do that."),
    BOTH_CONNECTED_SAME_CHANNEL(null);

    fun sendErrorMessage(channel: MessageChannel) {
        errorMessage?.let {
            channel.sendMessage(it).queue()
        }
    }

    companion object {
        fun from(evt: MessageReceivedEvent): VoiceStatus {
            val selfIsConnected = evt.guild.audioManager.isConnected
            val userIsConnected = evt.member!!.voiceState!!.inVoiceChannel()
            val differentChannel = evt.member!!.voiceState!!.channel != evt.guild.audioManager.connectedChannel
            return when {
                !userIsConnected && selfIsConnected -> SELF_CONNECTED_USER_DISCONNECTED
                !selfIsConnected && userIsConnected -> SELF_DISCONNECTED_USER_CONNECTED
                !selfIsConnected && !userIsConnected -> NEITHER_CONNECTED
                differentChannel -> BOTH_CONNECTED_DIFFERENT_CHANNEL
                else -> BOTH_CONNECTED_SAME_CHANNEL
            }
        }
    }
}

object AudioHandler : AudioPlayerManager by DefaultAudioPlayerManager() {
    init {
        AudioSourceManagers.registerRemoteSources(this)
        AudioSourceManagers.registerLocalSource(this)
    }

    inline fun loadTrack(
        query: String,
        channel: TextChannel,
        crossinline onLoad: (AudioTrack) -> Unit
    ): Future<Void> = loadItem(query, object : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) = onLoad(track)

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult && playlist.tracks.isNotEmpty()) {
                onLoad(playlist.selectedTrack ?: playlist.tracks.first())
            } else {
                channel.sendMessage("Can't load a track from a playlist URI.").queue()
            }
        }

        override fun noMatches() {
            channel.sendMessage("Couldn't find anything. Maybe you misspelled the query?").queue()
        }

        override fun loadFailed(exception: FriendlyException) {
            logger.error(exception.message ?: "Failed to load track. No error message available.")
            channel.sendMessage("Failed to load the track. The exception says `${exception.message}`.").queue()
        }
    })

    inline fun loadPlaylist(
        query: String,
        channel: TextChannel,
        crossinline onLoad: (AudioPlaylist) -> Unit
    ): Future<Void> = loadItem(query, object : AudioLoadResultHandler {
        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult) {
                channel.sendMessage("Can't load search results as a playlist!").queue()
            } else {
                onLoad(playlist)
            }
        }

        override fun trackLoaded(track: AudioTrack) {
            channel.sendMessage("Can't load a playlist from a track URI.").queue()
        }

        override fun noMatches() {
            channel.sendMessage("Couldn't find anything. Maybe you misspelled the query?").queue()
        }

        override fun loadFailed(exception: FriendlyException) {
            logger.warn(exception.message ?: "Failed to load playlist. No error message available.")
            channel.sendMessage("Failed to load the playlist. The exception says `${exception.message}`.").queue()
        }
    })
}

class GuildTrackManager(audioManager: AudioManager) : AudioEventAdapter() {
    private val player: AudioPlayer = AudioHandler.createPlayer().also {
        it.addListener(this)
    }
    private val queue = mutableListOf<AudioTrack>()
    val sendHandler = AudioPlayerSendHandler().also {
        audioManager.sendingHandler = it
    }
    var volume: Int
        get() = player.volume
        set(value) {
            player.volume = value.coerceIn(0..maxVolume)
        }
    val isPlaying: Boolean get() = player.playingTrack != null
    val isPaused: Boolean get() = isPlaying && player.isPaused
    val isNotPaused: Boolean get() = !isPaused

    fun reset() {
        stop()
        resume()
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

    fun sendQueueEmbed(channel: MessageChannel) {
        player.playingTrack?.let { track ->
            channel.sendEmbed {
                when (track) {
                    is YoutubeAudioTrack -> setThumbnail("https://img.youtube.com/vi/${track.info.identifier}/0.jpg")
                }
                setTitle("Now Playing")
                setDescription(track.infoString)
                val upNextList = queue
                    .take(queueListLength)
                    .joinToString("\n") { it.infoString }

                if (upNextList.isNotEmpty()) addField(
                    "Up Next",
                    buildString {
                        append(upNextList)
                        if (queue.size > queueListLength) {
                            append("\n plus ${queue.size - queueListLength} more...")
                        }
                    },
                    false
                )
            }.queue()
        } ?: channel.sendMessage("No songs are queued.").queue()
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (queue.isNotEmpty() && endReason == AudioTrackEndReason.FINISHED) {
            player.playTrack(queue.removeAt(0))
        }
    }

    inner class AudioPlayerSendHandler : AudioSendHandler {
        // behavior is the same whether we check for frames or not, so always return true
        override fun canProvide(): Boolean = true

        override fun provide20MsAudio(): ByteBuffer? = player.provide()?.data?.let(ByteBuffer::wrap)

        override fun isOpus() = true
    }

    companion object {
        private const val queueListLength = 8
        private const val maxVolume = 100

        private fun Duration.toBasicTimestamp(): String {
            val remainingMinutes = minusHours(toHours()).toMinutes()
            val remainingSeconds = minusMinutes(toMinutes()).seconds
            return if (toHours() == 0L) {
                "%d:%02d".format(remainingMinutes, remainingSeconds)
            } else "%d:%02d:%02d".format(toHours(), remainingMinutes, remainingSeconds)
        }

        private val AudioTrack.infoString: String
            get() {
                val durationString = Duration.ofMillis(duration).toBasicTimestamp()
                return if (state == AudioTrackState.PLAYING) {
                    val positionString = Duration.ofMillis(position).toBasicTimestamp()
                    "[${info.title}](${info.uri}) [$positionString/$durationString]"
                } else {
                    "[${info.title}](${info.uri}) [$durationString]"
                }
            }
    }
}

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

        override fun onUserSpeaking(user: User, speaking: Boolean) = Unit

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
    if (!audioManager.isConnected) {
        audioManager.openAudioConnection(voiceChannel, onConnect)
    }
}

fun handleVoiceStatus(evt: MessageReceivedEvent, shouldConnect: Boolean = false): Boolean =
    when (val status = VoiceStatus.from(evt)) {
        VoiceStatus.BOTH_CONNECTED_SAME_CHANNEL -> true
        VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> if (shouldConnect) {
            connectToVoiceChannel(evt.member!!.voiceState!!.channel!!)
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

defaultModule("Audio", defaultAccess = Access.Guild.All()) {
    command("joinVoice", "Joins the voice channel that the invoker is in.") {
        when (VoiceStatus.from(this)) {
            VoiceStatus.SELF_DISCONNECTED_USER_CONNECTED -> connectToVoiceChannel(member!!.voiceState!!.channel!!) {
                channel.sendMessage("Joined ${guild.audioManager.connectedChannel!!.name}.").queue()
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
            channel.sendMessage("Left ${guild.audioManager.connectedChannel!!.name}.").queue()
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

    command("queue", "Sends an embed with the list of songs in the queue.") {
        guild.trackManager.sendQueueEmbed(textChannel)
    }

    command("setVolume", "Sets the volume.") { volume: Int ->
        if (handleVoiceStatus(this)) {
            guild.trackManager.volume = volume
            channel.sendMessage("Set the volume to ${guild.trackManager.volume}%.").queue()
        }
    }

    listener<GuildVoiceMoveEvent> {
        if (guild.audioManager.connectedChannel != channelLeft) return@listener
        if (guild.audioManager.connectedChannel!!.members.all { it.user.isBot }) {
            leaveVoiceChannel(guild)
        }
    }

    listener<GuildVoiceLeaveEvent> {
        if (guild.audioManager.connectedChannel != channelLeft) return@listener
        if (guild.audioManager.connectedChannel!!.members.all { it.user.isBot }) {
            leaveVoiceChannel(guild)
        }
    }
}
