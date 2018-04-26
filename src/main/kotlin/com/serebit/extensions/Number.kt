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

fun Number.asMetricUnit(unit: String, base: Int = METRIC_BASE, decimalPoints: Int = 1): String = toDouble().let {
    val baseAsDouble = base.toDouble()
    val exponent = floor(log(it - 1, baseAsDouble)).roundToInt()
    val unitPrefix = listOf("", "K", "M", "G", "T", "P", "E")[exponent]
    "%.${decimalPoints}f $unitPrefix$unit".format(it / baseAsDouble.pow(exponent))
}

fun Number.toBasicTimestamp(): String = toInt().let {
    val hours by lazy { it / SECONDS_PER_HOUR }
    val minutes = it % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = it % SECONDS_PER_MINUTE
    if (hours == 0) {
        "%d:%02d".format(minutes, seconds)
    } else "%d:%02d:%02d".format(hours, minutes, seconds)
}

fun Number.toVerboseTimestamp(): String = toInt().let {
    val days by lazy { "${it / SECONDS_PER_DAY}d" }
    val hours by lazy { "${it % SECONDS_PER_DAY / SECONDS_PER_HOUR}h" }
    val minutes by lazy { "${it % SECONDS_PER_HOUR / SECONDS_PER_MINUTE}m" }
    val seconds = "${it % SECONDS_PER_MINUTE}s"
    when {
        it < SECONDS_PER_MINUTE -> seconds
        it < SECONDS_PER_HOUR -> "$minutes $seconds"
        it < SECONDS_PER_DAY -> "$hours $minutes $seconds"
        else -> "$days $hours $minutes $seconds"
    }
}

fun Number.asPercentageOf(total: Number): Int = (toDouble() / total.toDouble() * PERCENT_FACTOR).roundToInt()
