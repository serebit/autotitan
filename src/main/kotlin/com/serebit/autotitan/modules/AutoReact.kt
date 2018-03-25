package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.api.annotations.Listener
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.data.Emote
import com.serebit.autotitan.data.GuildResourcePairSet
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@Suppress("UNUSED")
class AutoReact : Module() {
    private val dataManager = DataManager(this::class)
    private val reactMap =
        dataManager.read("reacts.json") ?: GuildResourcePairSet<String, Emote>()

    @Command(
        description = "Adds an autoreact with the given emote for the given phrase.",
        locale = Locale.GUILD,
        memberPermissions = [Permission.MESSAGE_ADD_REACTION],
        splitLastParameter = false
    )
    fun addReact(evt: MessageReceivedEvent, emote: Emote, phrase: String) {
        reactMap.add(evt.guild, phrase to emote)
        dataManager.write("reacts.json", reactMap)
        evt.channel.sendMessage("Added reaction.").complete()
    }

    @Command(
        description = "Removes the autoreact for the given phrase from the list.",
        locale = Locale.GUILD,
        memberPermissions = [Permission.MESSAGE_ADD_REACTION],
        splitLastParameter = false
    )
    fun removeReact(evt: MessageReceivedEvent, emote: Emote, phrase: String) {
        reactMap.remove(evt.guild, phrase to emote)
        dataManager.write("reacts.json", reactMap)
        evt.channel.sendMessage("Removed reaction.").complete()
    }

    @Listener
    fun reactToMessage(evt: GuildMessageReceivedEvent) {
        if (evt.guild in reactMap) {
            reactMap[evt.guild]
                .filter { it.first in evt.message.contentRaw }
                .forEach { (_, emote) ->
                    when {
                        emote.isDiscordEmote -> evt.message.addReaction(
                            evt.jda.getEmoteById(emote.emoteIdValue!!)
                        ).queue()
                        emote.isUnicodeEmote -> evt.message.addReaction(emote.unicodeValue).queue()
                    }
                }
        }
    }
}
