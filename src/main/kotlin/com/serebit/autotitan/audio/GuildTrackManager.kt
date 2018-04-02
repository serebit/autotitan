package com.serebit.autotitan.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.serebit.extensions.jda.infoString
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.MessageChannel

class GuildTrackManager : AudioEventAdapter() {
    private val player: AudioPlayer = AudioHandler.createPlayer().also {
        it.addListener(this)
    }
    private val queue = mutableListOf<AudioTrack>()
    val sendHandler = AudioPlayerSendHandler()
    var volume: Int
        get() = player.volume
        set(value) {
            player.volume = value.coerceIn(0..maxVolume)
        }
    val isPlaying: Boolean get() = player.playingTrack != null
    val isNotPlaying get() = !isPlaying
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
                    .joinToString("\n", transform = AudioTrack::infoString)

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
            }.complete()
        } ?: channel.sendMessage("No songs are queued.").complete()
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
        private const val queueListLength = 8
        private const val maxVolume = 100
    }
}
