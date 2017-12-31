package com.serebit.autotitan.api.meta

/**
 * Represents the different access levels that commands may have.
 * Some access levels are guild-only.
 */
enum class Access {
    ALL,
    GUILD_OWNER,
    BOT_OWNER,
    RANK_ABOVE,
    RANK_SAME,
    RANK_BELOW
}
