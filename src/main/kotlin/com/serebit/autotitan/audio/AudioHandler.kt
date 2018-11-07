package com.serebit.autotitan.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.serebit.logkat.Logger
import net.dv8tion.jda.core.entities.TextChannel
import java.util.concurrent.Future

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
            Logger.error(exception.message ?: "Failed to load track. No error message available.")
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
            Logger.warn(exception.message ?: "Failed to load playlist. No error message available.")
            channel.sendMessage("Failed to load the playlist. The exception says `${exception.message}`.").queue()
        }
    })
}
