package com.serebit.autotitan.extensions.standard.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import com.serebit.autotitan.api.annotations.ListenerFunction
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.managers.AudioManager
import org.apache.commons.validator.routines.UrlValidator

@ExtensionClass
class Audio {
    private val urlValidator = UrlValidator(arrayOf("http", "https"))
    private val playerManager = DefaultAudioPlayerManager()
    private val audioManagers = mutableMapOf<Guild, GuildMusicManager>()

    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    @CommandFunction(
            description = "Joins the voice channel that the invoker is in.",
            locale = Locale.GUILD
    )
    fun joinVoice(evt: MessageReceivedEvent) {
        if (evt.member.voiceState.inVoiceChannel()) {
            connectToVoiceChannel(evt.guild.audioManager, evt.member.voiceState.channel)
            evt.channel.sendMessage("Now connected to ${evt.member.voiceState.channel.name}.")
        } else {
            evt.channel.sendMessage("You need to be in a voice channel for me to do that.").complete()
        }
    }

    @CommandFunction(
            description = "Leaves the voice channel that the bot is in.",
            locale = Locale.GUILD
    )
    fun leaveVoice(evt: MessageReceivedEvent) {
        if (evt.guild.audioManager.isConnected) {
            val audioPlayer = evt.guild.getMusicManager()
            audioPlayer.scheduler.next()
            evt.guild.audioManager.closeAudioConnection()
        }
    }

    @ListenerFunction
    fun leaveVoiceAutomatically(evt: GuildVoiceLeaveEvent) {
        if (evt.guild.audioManager.connectedChannel != evt.channelLeft) return
        val nobodyLeft = evt.guild.audioManager.connectedChannel.members.size == 1
        if (evt.guild.audioManager.isConnected && nobodyLeft) {
            val audioPlayer = evt.guild.getMusicManager()
            audioPlayer.scheduler.stop()
            evt.guild.audioManager.closeAudioConnection()
        }
    }

    @ListenerFunction
    fun leaveVoiceAutomatically(evt: GuildVoiceMoveEvent) {
        if (evt.guild.audioManager.connectedChannel != evt.channelLeft) return
        val nobodyLeft = evt.guild.audioManager.connectedChannel.members.size == 1
        if (evt.guild.audioManager.isConnected && nobodyLeft) {
            evt.guild.getMusicManager().scheduler.stop()
            evt.guild.audioManager.closeAudioConnection()
        }
    }

    @CommandFunction(
            description = "Plays a URL, or searches YouTube for the given search terms.",
            locale = Locale.GUILD,
            delimitFinalParameter = false
    )
    fun play(evt: MessageReceivedEvent, linkOrSearchTerms: String) {
        if (!validVoiceStatus(evt)) return
        val audioManager = evt.guild.getMusicManager()
        val formattedLinkOrSearchTerms = if (urlValidator.isValid(linkOrSearchTerms)) {
            linkOrSearchTerms
        } else {
            "ytsearch:$linkOrSearchTerms"
        }
        playerManager.loadItemOrdered(audioManager, formattedLinkOrSearchTerms, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                evt.channel.sendMessage("Adding ${track.info.title} to queue.").complete()
                audioManager.scheduler.queue(track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                val firstTrack = playlist.selectedTrack ?: playlist.tracks[0]
                evt.channel.sendMessage("Adding ${firstTrack.info.title} to queue.").complete()
                audioManager.scheduler.queue(firstTrack)
            }

            override fun noMatches() {
                evt.channel.sendMessage("Nothing found.").complete()
            }

            override fun loadFailed(exception: FriendlyException) {
                evt.channel.sendMessage("Could not queue: ${exception.message}").complete()
            }
        })
    }

    @CommandFunction(
            description = "Skips the currently playing song.",
            locale = Locale.GUILD
    )
    fun skip(evt: MessageReceivedEvent) {
        if (!validVoiceStatus(evt)) return
        val audioManager = evt.guild.getMusicManager()
        audioManager.scheduler.next()
        evt.channel.sendMessage("Skipped to next track.").complete()
    }

    @CommandFunction(
            description = "Stops playing music and clears the queue.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.VOICE_MUTE_OTHERS)
    )
    fun stop(evt: MessageReceivedEvent) {
        evt.guild.getMusicManager().scheduler.stop()
    }

    @CommandFunction(
            description = "Pauses the currently playing song.",
            locale = Locale.GUILD
    )
    fun pause(evt: MessageReceivedEvent) {
        if (!validVoiceStatus(evt)) return
        val audioManager = evt.guild.getMusicManager()
        if (audioManager.scheduler.pause()) {
            evt.channel.sendMessage("Paused.").complete()
        }
    }

    @CommandFunction(
            description = "Resumes the currently playing song.",
            locale = Locale.GUILD
    )
    fun resume(evt: MessageReceivedEvent) {
        if (!validVoiceStatus(evt)) return
        if (evt.guild.getMusicManager().scheduler.resume()) {
            evt.channel.sendMessage("Resumed.").complete()
        }
    }

    @CommandFunction(
            description = "Sends an embed with the list of songs in the queue.",
            locale = Locale.GUILD
    )
    fun queue(evt: MessageReceivedEvent) {
        if (evt.guild.getMusicManager().player.playingTrack != null) {
            val audioManager = evt.guild.getMusicManager()
            val embed = EmbedBuilder().apply {
                setTitle("Queue", null)
                setColor(evt.guild.getMember(evt.jda.selfUser).color)
                setThumbnail(evt.jda.selfUser.effectiveAvatarUrl)
                addField("Now Playing", audioManager.player.playingTrack.info.title, false)
                if (audioManager.scheduler.queue.isNotEmpty()) addField(
                        "Up Next",
                        audioManager.scheduler.queue.joinToString("\n") { it.info.title },
                        false
                )
            }.build()
            evt.channel.sendMessage(embed).complete()
        } else {
            evt.channel.sendMessage("There's nothing queued at the moment.").complete()
        }
    }

    @CommandFunction(
            description = "Sets the volume.",
            locale = Locale.GUILD
    )
    fun setVolume(evt: MessageReceivedEvent, volume: Int) {
        if (!validVoiceStatus(evt)) return
        val newVolume = when {
            volume > 100 -> 100
            volume < 0 -> 0
            else -> volume
        }
        evt.guild.getMusicManager().player.volume = newVolume
        evt.channel.sendMessage("Set volume to $newVolume.").complete()
    }


    private fun Guild.getMusicManager(): GuildMusicManager {
        val audioManager = audioManagers.getOrElse(this, {
            val newManager = GuildMusicManager(playerManager)
            audioManagers.put(this, newManager)
            newManager
        })
        this.audioManager.sendingHandler = audioManager.sendHandler
        return audioManager
    }

    private fun connectToVoiceChannel(audioManager: AudioManager, voiceChannel: VoiceChannel) {
        if (!audioManager.isConnected && !audioManager.isAttemptingToConnect) {
            audioManager.openAudioConnection(voiceChannel)
        }
    }

    private fun validVoiceStatus(evt: MessageReceivedEvent): Boolean {
        val isConnected = evt.guild.audioManager.isConnected
        val sameChannel = evt.member.voiceState.channel == evt.guild.audioManager.connectedChannel
        if (!isConnected) {
            evt.channel.sendMessage("I need to be in a voice channel to do that.").complete()
            return false
        }
        if (!sameChannel) {
            evt.channel.sendMessage("We need to be in the same voice channel for you to do that.").complete()
            return false
        }
        return true
    }
}
