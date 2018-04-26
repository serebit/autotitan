@file:JvmName("StringExtensions")

package com.serebit.extensions

import net.dv8tion.jda.core.entities.MessageEmbed

fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

fun String.limitLengthTo(max: Int): String {
    val trimmedString = replace("(\\s){2,}".toRegex(), "$1$1")
    return if (trimmedString.length > max) {
        trimmedString
            .replace("(\\s){2,}".toRegex(), "$1$1")
            .substring(0 until MessageEmbed.VALUE_MAX_LENGTH - 1) + '\u2026'
    } else trimmedString
}
