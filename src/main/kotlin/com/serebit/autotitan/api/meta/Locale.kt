package com.serebit.autotitan.api.meta

/**
 * Represents the different locales that commands may be executed within.
 */
internal enum class Locale(val description: String) {
    ALL("Both private channels and guilds"),
    PRIVATE_CHANNEL("Private channels only"),
    GUILD("Guilds only")
}
