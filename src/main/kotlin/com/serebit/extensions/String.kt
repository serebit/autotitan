@file:JvmName("StringExtensions")

package com.serebit.extensions

fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

fun String.toCharOrNull() = if (length == 1) this[0] else null
