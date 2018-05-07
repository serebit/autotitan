@file:JvmName("StringExtensions")

package com.serebit.extensions

import net.dv8tion.jda.core.entities.MessageEmbed

fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

fun String.limitLengthTo(max: Int): String = if (length > max) {
    substring(0 until MessageEmbed.VALUE_MAX_LENGTH - 1) + '\u2026'
} else this

fun String.trimWhitespace(): String = replace("(\\s){2,}".toRegex(), "$1$1")
