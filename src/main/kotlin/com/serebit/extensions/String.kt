@file:JvmName("StringExtensions")

package com.serebit.extensions

import com.vdurmont.emoji.EmojiParser
import net.dv8tion.jda.core.entities.MessageEmbed

fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

fun String.toCharOrNull() = if (length == 1) this[0] else null

val String.isUnicodeEmoji: Boolean get() = EmojiParser.extractEmojis(this).size == 1

fun String.limitLengthTo(max: Int): String {
    val trimmedString = replace("(\\s){2,}".toRegex(), "$1$1")
    return if (trimmedString.length > max) {
        trimmedString
            .replace("(\\s){2,}".toRegex(), "$1$1")
            .substring(0 until MessageEmbed.VALUE_MAX_LENGTH - 1) + '\u2026'
    } else trimmedString
}
