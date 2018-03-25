@file:JvmName("StringExtensions")

package com.serebit.extensions

import khttp.get

private val validUnicodeCodePoints = get("https://raw.githubusercontent.com/emojione/emojione/master/emoji.json")
    .jsonObject
    .keySet()
    .map {
        it.split("-").map { it.toInt(16) }.toIntArray()
    }
    .toSet()

fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

fun String.toCharOrNull() = if (length == 1) this[0] else null

val String.isUnicodeEmote: Boolean
    get() {
        val codePoints = codePoints().toArray()
        return validUnicodeCodePoints.any { it.contentEquals(codePoints) }
    }
