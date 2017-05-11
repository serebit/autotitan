package com.serebit.autotitan.extensions.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

data class Data(val player: AudioPlayer, var playingTrack: AudioTrack?)