package com.serebit.extensions.jda

import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

private const val SECONDS_PER_DAY = 86400L
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_MINUTE = 60L

fun Long.asMetricUnit(unit: String, base: Double = 1000.0, decimalPoints: Int = 1): String {
    val exponent = ceil(log(this.toDouble(), base)).toInt() - 1
    val unitPrefix = listOf("", "k", "M", "G", "T", "P", "E")[exponent]
    return "%.${decimalPoints}f $unitPrefix$unit".format(this / base.pow(exponent))
}

fun Long.toBasicTimestamp(): String {
    val totalSeconds = this / 1000
    val hours = (totalSeconds / 3600).toInt()
    val minutes = (totalSeconds % 3600 / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return when (hours) {
        0 -> "%d:%02d".format(minutes, seconds)
        else -> "%d:%02d:%02d".format(hours, minutes, seconds)
    }
}

fun Long.toVerboseTimestamp(): String {
    val days = "${this / SECONDS_PER_DAY}d"
    val hours = "${this % SECONDS_PER_DAY / SECONDS_PER_HOUR}h"
    val minutes = "${this % SECONDS_PER_HOUR / SECONDS_PER_MINUTE}m"
    val seconds = "${this % SECONDS_PER_MINUTE}s"
    return when {
        this < SECONDS_PER_MINUTE -> seconds
        this < SECONDS_PER_HOUR -> "$minutes $seconds"
        this < SECONDS_PER_DAY -> "$hours $minutes $seconds"
        else -> "$days $hours $minutes $seconds"
    }
}

fun Long.asPercentageOf(total: Long): Int = (this.toDouble() / total.toDouble() * 100).roundToInt()
