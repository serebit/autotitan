package com.serebit.autotitan.api.meta

sealed class Access(val description: String) {
    object All : Access("Anyone")
    object BotOwner : Access("Bot owner only")

    sealed class Private(description: String) : Access(description) {
        object All : Private("In private messages only")
        object BotOwner : Private("Bot owner in private messages only")
    }

    sealed class Guild(description: String) : Access(description) {
        object All : Guild("Servers only")
        object BotOwner : Guild("Bot owner in servers only")
        object GuildOwner : Guild("Server owner only")
        object RankAbove : Guild("Anyone with their top role above the bot's top role")
        object RankSame : Guild("Anyone with the same top role as the bot's top role")
        object RankBelow : Guild("Anyone with their top role below the bot's top role")
    }
}
