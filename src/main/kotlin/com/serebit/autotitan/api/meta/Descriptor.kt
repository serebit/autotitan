package com.serebit.autotitan.api.meta

import com.serebit.autotitan.config

data class Descriptor(val name: String, val description: String) {
    val invocation get() = "${config.prefix}$name"

    fun matches(message: String): Boolean = message.substringBefore(" ") == invocation
}
