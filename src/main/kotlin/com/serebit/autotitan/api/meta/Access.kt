package com.serebit.autotitan.api.meta

/**
 * Represents the different access levels that commands may have.
 * Some access levels are guild-only.
 */
enum class Access(val description: String) {
    ALL("Anyone"),
    BOT_OWNER("Bot owner only"),
    PRIVATE_ALL("Private messages only"),
    PRIVATE_BOT_OWNER("Bot owner in private messages only"),
    GUILD_ALL("Servers only"),
    GUILD_BOT_OWNER("Bot owner in servers only"),
    GUILD_OWNER("Server owner only"),
    GUILD_RANK_ABOVE("Anyone with their top role above the bot's top role"),
    GUILD_RANK_SAME("Anyone with the same top role as the bot's top role"),
    GUILD_RANK_BELOW("Anyone with their top role below the bot's top role")
}
