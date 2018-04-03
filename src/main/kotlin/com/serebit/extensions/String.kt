@file:JvmName("StringExtensions")

package com.serebit.extensions

import khttp.get
import net.dv8tion.jda.core.entities.MessageEmbed

private const val EMOJIONE_CODEPOINT_RADIX = 16
private val validUnicodeCodePoints = get("https://raw.githubusercontent.com/emojione/emojione/master/emoji.json")
    .jsonObject
    .keySet()
    .map {
        it.split("-").map { it.toInt(EMOJIONE_CODEPOINT_RADIX) }.toIntArray()
    }
    .toSet()

fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

fun String.toCharOrNull() = if (length == 1) this[0] else null

val String.isUnicodeEmote: Boolean
    get() {
        val codePoints = codePoints().toArray()
        return validUnicodeCodePoints.any { it.contentEquals(codePoints) }
    }

fun String.limitLengthTo(max: Int) = if (length > max) {
    substring(0 until MessageEmbed.VALUE_MAX_LENGTH) + '\u2026'
} else this
