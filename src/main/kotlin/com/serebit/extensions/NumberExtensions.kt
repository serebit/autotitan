package com.serebit.extensions

import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

private const val SECONDS_PER_DAY = 86400L
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_MINUTE = 60L
private const val DEFAULT_METRIC_BASE = 1000.0
private const val PERCENT_FACTOR = 100

fun Number.asMetricUnit(unit: String, base: Double = DEFAULT_METRIC_BASE, decimalPoints: Int = 1): String {
    val asDouble = toDouble()
    val exponent = ceil(log(asDouble, base)).toInt() - 1
    val unitPrefix = listOf("", "K", "M", "G", "T", "P", "E")[exponent]
    return "%.${decimalPoints}f $unitPrefix$unit".format(asDouble / base.pow(exponent))
}

fun Number.toBasicTimestamp(): String {
    val asDouble = toDouble()
    val hours = (asDouble / SECONDS_PER_HOUR).toInt()
    val minutes = (asDouble % SECONDS_PER_HOUR / SECONDS_PER_MINUTE).toInt()
    val seconds = (asDouble % SECONDS_PER_MINUTE).toInt()
    return when (hours) {
        0 -> "%d:%02d".format(minutes, seconds)
        else -> "%d:%02d:%02d".format(hours, minutes, seconds)
    }
}

fun Number.toVerboseTimestamp(): String {
    val asDouble = toDouble()
    val days = "${asDouble / SECONDS_PER_DAY}d"
    val hours = "${asDouble % SECONDS_PER_DAY / SECONDS_PER_HOUR}h"
    val minutes = "${asDouble % SECONDS_PER_HOUR / SECONDS_PER_MINUTE}m"
    val seconds = "${asDouble % SECONDS_PER_MINUTE}s"
    return when {
        asDouble < SECONDS_PER_MINUTE -> seconds
        asDouble < SECONDS_PER_HOUR -> "$minutes $seconds"
        asDouble < SECONDS_PER_DAY -> "$hours $minutes $seconds"
        else -> "$days $hours $minutes $seconds"
    }
}

fun Number.asPercentageOf(total: Number): Int = (toDouble() / total.toDouble() * PERCENT_FACTOR).roundToInt()
