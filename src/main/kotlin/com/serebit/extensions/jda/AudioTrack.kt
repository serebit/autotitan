package com.serebit.extensions.jda

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.serebit.extensions.toBasicTimestamp

private const val millisecondsPerSecond = 1000

val AudioTrack.infoString: String get() {
    val durationString = (duration / millisecondsPerSecond).toBasicTimestamp()
    return "[${info.title}](${info.uri}) [$durationString]"
}

val AudioTrack.infoStringWithPosition: String get() {
    val positionString = (position / millisecondsPerSecond).toBasicTimestamp()
    val durationString = (duration / millisecondsPerSecond).toBasicTimestamp()
    return "[${info.title}](${info.uri}) [$positionString/$durationString]"
}
