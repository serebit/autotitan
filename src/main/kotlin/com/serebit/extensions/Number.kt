package com.serebit.extensions

import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

private const val SECONDS_PER_DAY = 86400
private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60
private const val METRIC_BASE = 1000
private const val PERCENT_FACTOR = 100

fun Long.asMetricUnit(unit: String, base: Int = METRIC_BASE, decimalPoints: Int = 1): String {
    val baseAsFloat = base.toFloat()
    val exponent = floor(log(toFloat() - 1, baseAsFloat)).roundToInt()
    val unitPrefix = listOf("", "K", "M", "G", "T", "P", "E")[exponent]
    return "%.${decimalPoints}f $unitPrefix$unit".format(this / baseAsFloat.pow(exponent))
}

fun Long.toBasicTimestamp(): String {
    val hours by lazy { this / SECONDS_PER_HOUR }
    val minutes = this % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = this % SECONDS_PER_MINUTE
    return if (hours == 0L) {
        "%d:%02d".format(minutes, seconds)
    } else "%d:%02d:%02d".format(hours, minutes, seconds)
}

fun Long.toVerboseTimestamp(): String {
    val days by lazy { "${this / SECONDS_PER_DAY}d" }
    val hours by lazy { "${this % SECONDS_PER_DAY / SECONDS_PER_HOUR}h" }
    val minutes by lazy { "${this % SECONDS_PER_HOUR / SECONDS_PER_MINUTE}m" }
    val seconds = "${this % SECONDS_PER_MINUTE}s"
    return when {
        this < SECONDS_PER_MINUTE -> seconds
        this < SECONDS_PER_HOUR -> "$minutes $seconds"
        this < SECONDS_PER_DAY -> "$hours $minutes $seconds"
        else -> "$days $hours $minutes $seconds"
    }
}

fun Long.asPercentageOf(total: Long): Long = this / total * PERCENT_FACTOR
