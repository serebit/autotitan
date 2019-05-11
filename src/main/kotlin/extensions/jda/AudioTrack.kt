package com.serebit.autotitan.extensions.jda

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState
import com.serebit.autotitan.extensions.toBasicTimestamp

private const val MILLISECONDS_PER_SECOND = 1000

val AudioTrack.infoString: String
    get() {
        val durationString = (duration / MILLISECONDS_PER_SECOND).toBasicTimestamp()
        return if (state == AudioTrackState.PLAYING) {
            val positionString = (position / MILLISECONDS_PER_SECOND).toBasicTimestamp()
            "[${info.title}](${info.uri}) [$positionString/$durationString]"
        } else {
            "[${info.title}](${info.uri}) [$durationString]"
        }
    }
