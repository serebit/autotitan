package com.serebit.autotitan.api.meta

/**
 * Represents the different access levels that commands may have.
 * Some access levels are guild-only.
 */
enum class Access(val description: String) {
    ALL("Anyone"),
    GUILD_OWNER("Guild owner only"),
    BOT_OWNER("Bot owner only"),
    RANK_ABOVE("Anyone with their top role above the bot's top role"),
    RANK_SAME("Anyone with the same top role as the bot's top role"),
    RANK_BELOW("Anyone with their top role below the bot's top role")
}
