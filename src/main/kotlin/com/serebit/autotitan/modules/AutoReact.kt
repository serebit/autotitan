package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.api.annotations.Listener
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.data.Emote
import com.serebit.autotitan.data.GuildResourceMap
import com.serebit.extensions.jda.addReaction
import com.serebit.extensions.jda.chunkedBy
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.limitLengthTo
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.Clock
import java.time.OffsetDateTime

@Suppress("UNUSED")
class AutoReact : Module(isOptional = true) {
    private val dataManager = DataManager(this::class)
    private val reactMap = dataManager.read("reacts.json") ?: GuildResourceMap<String, MutableList<EmoteData>>()

    @Command(
        description = "Adds an autoreact with the given emote for the given word.",
        locale = Locale.GUILD,
        memberPermissions = [Permission.MESSAGE_ADD_REACTION]
    )
    fun addReact(evt: MessageReceivedEvent, word: String, emote: Emote) {
        val value = reactMap[evt.guild].getOrPut(word, ::mutableListOf)
        when {
            value.size >= maxReactionsPerMessage ->
                evt.channel.sendMessage("There are already 20 reactions for that word.").complete()
            !emote.canInteract(evt.channel) -> evt.channel.sendMessage("I can't use that emote.").complete()
            value.add(EmoteData.from(emote, evt)) -> {
                dataManager.write("reacts.json", reactMap)
                evt.channel.sendMessage("Added reaction.").complete()
            }
        }
    }

    @Command(
        description = "Removes the autoreact for the given word from the list.",
        locale = Locale.GUILD,
        memberPermissions = [Permission.MESSAGE_ADD_REACTION]
    )
    fun removeReact(evt: MessageReceivedEvent, word: String, emote: Emote) {
        val guildReacts = reactMap[evt.guild]
        when {
            word !in guildReacts -> evt.channel.sendMessage("There are no autoreacts for that word.").complete()
            guildReacts[word]?.none { it.emote == emote } ?: false ->
                evt.channel.sendMessage("That emote is not an autoreact for that word.").complete()
            guildReacts[word]?.removeIf { it.emote == emote } ?: false -> {
                dataManager.write("reacts.json", reactMap)
                evt.channel.sendMessage("Removed reaction.")
            }
        }
    }

    @Command(
        description = "Deletes all autoreacts from the server.",
        locale = Locale.GUILD,
        memberPermissions = [Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_SERVER],
        splitLastParameter = false
    )
    fun clearReacts(evt: MessageReceivedEvent) {
        reactMap[evt.guild].clear()
        evt.channel.sendMessage("Cleared all autoreacts from this server.").complete()
    }

    @Command(description = "Sends a list of autoreacts for the server to the command invoker.", locale = Locale.GUILD)
    fun reactList(evt: MessageReceivedEvent) {
        val reacts = reactMap[evt.guild]
        evt.channel.sendMessage("Sending a reaction list in PMs.").queue()
        if (reacts.isNotEmpty()) {
            evt.author.openPrivateChannel().queue({ privateChannel ->
                reacts
                    .map { (word, emotes) ->
                        word.limitLengthTo(MessageEmbed.TITLE_MAX_LENGTH) to
                            emotes.joinToString("") { it.emote.toString(evt.jda) }
                    }
                    .chunkedBy(MessageEmbed.EMBED_MAX_LENGTH_BOT) { it.first.length + it.second.length }
                    .forEach { embeds ->
                        privateChannel.sendEmbed {
                            embeds.forEach { addField(it.first, it.second, false) }
                        }.queue()
                    }
            }, { evt.channel.sendMessage(it.message).complete() })
        } else evt.channel.sendMessage("This server has no autoreacts saved.").complete()
    }

    @Listener
    fun reactToMessage(evt: GuildMessageReceivedEvent) {
        if (evt.guild in reactMap && evt.author != evt.jda.selfUser) {
            reactMap[evt.guild]
                .filter { it.key in evt.message.contentRaw }
                .values
                .flatten()
                .forEach { evt.message.addReaction(it.emote).queue() }
        }
    }

    private data class EmoteData(
        val emote: Emote,
        val creationTimestamp: String,
        val authorId: Long,
        val channelId: Long
    ) {
        val creationTime: OffsetDateTime get() = OffsetDateTime.parse(creationTimestamp)

        companion object {
            fun from(emote: Emote, evt: MessageReceivedEvent): EmoteData = EmoteData(
                emote,
                OffsetDateTime.now(Clock.systemUTC()).toString(),
                evt.author.idLong,
                evt.channel.idLong
            )
        }
    }

    companion object {
        private const val maxReactionsPerMessage = 20
        private const val maxFieldsPerReactListEmbed = 8
    }
}
