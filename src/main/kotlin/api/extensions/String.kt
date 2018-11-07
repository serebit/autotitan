@file:JvmName("StringExtensions")

package com.serebit.autotitan.api.extensions

fun String.limitLengthTo(max: Int): String = if (length > max) {
    substring(0 until max - 1) + '\u2026'
} else this
