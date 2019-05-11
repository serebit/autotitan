package com.serebit.autotitan.extensions

import java.util.*

private val random = Random()

internal fun <T> List<T>.randomEntry() = if (isNotEmpty()) {
    get(random.nextInt(size))
} else null
