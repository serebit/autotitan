package com.serebit.extensions

import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

private const val SECONDS_PER_DAY = 86400
private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60
private const val DEFAULT_METRIC_BASE = 1000
private const val PERCENT_FACTOR = 100

fun Number.asMetricUnit(unit: String, base: Int = DEFAULT_METRIC_BASE, decimalPoints: Int = 1): String {
    val asDouble = toDouble()
    val baseAsDouble = base.toDouble()
    val exponent = floor(log(asDouble - 1, baseAsDouble)).roundToInt()
    val unitPrefix = listOf("", "K", "M", "G", "T", "P", "E")[exponent]
    return "%.${decimalPoints}f $unitPrefix$unit".format(asDouble / baseAsDouble.pow(exponent))
}

fun Number.toBasicTimestamp(): String {
    val asInt = toInt()
    val hours = asInt / SECONDS_PER_HOUR
    val minutes = asInt % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = asInt % SECONDS_PER_MINUTE
    return if (hours == 0) {
        "%d:%02d".format(minutes, seconds)
    } else "%d:%02d:%02d".format(hours, minutes, seconds)
}

fun Number.toVerboseTimestamp(): String {
    val asInt = toInt()
    val days = "${asInt / SECONDS_PER_DAY}d"
    val hours = "${asInt % SECONDS_PER_DAY / SECONDS_PER_HOUR}h"
    val minutes = "${asInt % SECONDS_PER_HOUR / SECONDS_PER_MINUTE}m"
    val seconds = "${asInt % SECONDS_PER_MINUTE}s"
    return when {
        asInt < SECONDS_PER_MINUTE -> seconds
        asInt < SECONDS_PER_HOUR -> "$minutes $seconds"
        asInt < SECONDS_PER_DAY -> "$hours $minutes $seconds"
        else -> "$days $hours $minutes $seconds"
    }
}

fun Number.asPercentageOf(total: Number): Int = (toDouble() / total.toDouble() * PERCENT_FACTOR).roundToInt()
