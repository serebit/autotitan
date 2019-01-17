
import com.serebit.autotitan.api.command
import com.serebit.autotitan.api.extensions.MESSAGE_EMBED_MAX_FIELDS
import com.serebit.autotitan.api.extensions.chunkedBy
import com.serebit.autotitan.api.extensions.limitLengthTo
import com.serebit.autotitan.api.extensions.sendEmbed
import com.serebit.autotitan.api.listener
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.Emote
import com.serebit.autotitan.data.GuildResourceMap
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.RestAction
import java.time.Clock
import java.time.OffsetDateTime

fun Message.addReaction(emote: Emote): RestAction<Void>? = when (emote) {
    is Emote.Jda -> jda.getEmoteById(emote.value)?.let { addReaction(it) }
    is Emote.Unicode -> addReaction(emote.value)
}

data class EmoteData(
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

val maxReactionsPerMessage = 20

module("Autoreact", isOptional = true, defaultAccess = Access.Guild.All(Permission.MESSAGE_ADD_REACTION)) {
    val reactMap = dataManager.readOrDefault("reacts.json") {
        GuildResourceMap<String, MutableList<EmoteData>>()
    }

    command("addReact", "Adds an autoreact with the given emote for the given word.") { word: String, emote: Emote ->
        if (guild.idLong !in reactMap) reactMap[guild.idLong] = mutableMapOf()
        val value = reactMap[guild.idLong]!!.getOrPut(word, ::mutableListOf)
        when {
            value.size >= maxReactionsPerMessage ->
                channel.sendMessage("There are already $maxReactionsPerMessage reactions for that word.").queue()
            !emote.canInteract(channel) -> channel.sendMessage("I can't use that emote.").queue()
            value.add(EmoteData.from(emote, this)) -> {
                dataManager.write("reacts.json", reactMap)
                channel.sendMessage("Added reaction.").queue()
            }
        }
    }

    command("removeReact", "Removes the autoreact for the given word from the list.") { word: String, emote: Emote ->
        if (guild.idLong !in reactMap) reactMap[guild.idLong] = mutableMapOf()
        val guildReacts = reactMap[guild.idLong]!!
        when {
            word !in guildReacts -> channel.sendMessage("There are no autoreacts for that word.").queue()
            guildReacts[word]?.none { it.emote == emote } ?: false ->
                channel.sendMessage("That emote is not an autoreact for that word.").queue()
            guildReacts[word]?.removeIf { it.emote == emote } ?: false -> {
                dataManager.write("reacts.json", reactMap)
                channel.sendMessage("Removed reaction.")
            }
        }
    }

    command(
        "clearReacts",
        "Deletes all autoreacts from the server.",
        Access.Guild.All(Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_SERVER)
    ) {
        if (guild.idLong !in reactMap) reactMap[guild.idLong] = mutableMapOf()
        reactMap[guild.idLong]!!.clear()
        dataManager.write("reacts.json", reactMap)
        channel.sendMessage("Cleared all autoreacts from this server.").queue()
    }

    command("reactList", "Sends a list of autoreacts for the server to the command invoker.", Access.Guild.All()) {
        if (guild.idLong !in reactMap) reactMap[guild.idLong] = mutableMapOf()
        val reacts = reactMap[guild.idLong]!!
        channel.sendMessage("Sending a reaction list in PMs.").queue()
        if (reacts.isNotEmpty()) {
            author.openPrivateChannel().queue({ privateChannel ->
                reacts
                    .map { (word, emotes) ->
                        word.limitLengthTo(MessageEmbed.TITLE_MAX_LENGTH) to
                                emotes.joinToString("") { it.emote.asMention(guild) }
                    }
                    .chunkedBy(MessageEmbed.EMBED_MAX_LENGTH_BOT, MESSAGE_EMBED_MAX_FIELDS) {
                        it.first.length + it.second.length
                    }
                    .forEach { embeds ->
                        privateChannel.sendEmbed {
                            embeds.forEach { addField(it.first, it.second, false) }
                        }.queue()
                    }
            }, { channel.sendMessage(it.message).queue() })
        } else channel.sendMessage("This server has no autoreacts saved.").queue()
    }

    listener<MessageReceivedEvent> {
        if (guild?.idLong in reactMap && author != jda.selfUser) {
            reactMap[guild.idLong]!!
                .filter { it.key in message.contentRaw }
                .values
                .flatten()
                .forEach { message?.addReaction(it.emote)?.queue() }
        }
    }
}
