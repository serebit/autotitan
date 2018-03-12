package com.serebit.autotitan.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.entities.MessageChannel
import java.util.concurrent.Future

object AudioHandler : AudioPlayerManager by DefaultAudioPlayerManager() {
    init {
        AudioSourceManagers.registerRemoteSources(this)
        AudioSourceManagers.registerLocalSource(this)
    }

    inline fun loadTrack(
        query: String,
        channel: MessageChannel,
        crossinline onLoad: (AudioTrack) -> Unit
    ): Future<Void> = loadItem(query, object : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) = onLoad(track)

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult) {
                onLoad(playlist.selectedTrack ?: playlist.tracks[0])
            } else {
                channel.sendMessage("Can't load a track from a playlist URI!").complete()
            }
        }

        override fun noMatches() {
            channel.sendMessage("Couldn't find anything. Maybe you misspelled the query?").complete()
        }

        override fun loadFailed(exception: FriendlyException) {
            Logger.error(exception.message ?: "Failed to load track. No error message available.")
            channel.sendMessage("Failed to load the track. The exception says `${exception.message}`.").complete()
        }
    })

    inline fun loadPlaylist(
        query: String,
        channel: MessageChannel,
        crossinline onLoad: (AudioPlaylist) -> Unit
    ): Future<Void> = loadItem(query, object : AudioLoadResultHandler {
        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult) {
                channel.sendMessage("Can't load search results as a playlist!").complete()
            } else {
                onLoad(playlist)
            }
        }

        override fun trackLoaded(track: AudioTrack) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun noMatches() {
            channel.sendMessage("Couldn't find anything. Maybe you misspelled the query?").complete()
        }

        override fun loadFailed(exception: FriendlyException) {
            Logger.error(exception.message ?: "Failed to load playlist. No error message available.")
            channel.sendMessage("Failed to load the playlist. The exception says `${exception.message}`.").complete()
        }
    })
}
