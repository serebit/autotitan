import com.fasterxml.jackson.annotation.JsonIgnore
import com.serebit.autotitan.api.*
import com.serebit.autotitan.extensions.chunkedBy
import com.serebit.autotitan.extensions.limitLengthTo
import com.serebit.autotitan.extensions.sendEmbed
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import java.time.Clock
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun Message.addReaction(emote: Emote): RestAction<Void>? = when (emote) {
    is Emote.Jda -> jda.getEmoteById(emote.emoteIdValue)?.let { addReaction(it) }
    is Emote.Unicode -> addReaction(emote.unicodeValue)
}

data class EmoteData(
    val emote: Emote,
    val creationTimestamp: String,
    val authorId: Long,
    val channelId: Long
) {
    @JsonIgnore
    val creationTime: OffsetDateTime = OffsetDateTime.parse(creationTimestamp)

    companion object {
        fun from(emote: Emote, evt: MessageReceivedEvent): EmoteData = EmoteData(
            emote,
            OffsetDateTime.now(Clock.systemUTC()).format(DateTimeFormatter.ISO_INSTANT),
            evt.author.idLong,
            evt.channel.idLong
        )
    }
}

val maxReactionsPerMessage = 20

optionalModule("AutoReact", defaultAccess = Access.Guild.All(Permission.MESSAGE_ADD_REACTION)) {
    val reactMap = dataManager.readOrDefault("reacts.json") {
        GuildResourceMap<String, MutableList<EmoteData>>()
    }

    group("react") {
        command("add", "Adds an autoreact with the given emote for the given word.") { word: String, emote: Emote ->
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

        command("remove", "Removes the autoreact for the given word from the list.") { word: String, emote: Emote ->
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
            "clear",
            "Deletes all autoreacts from the server.",
            Access.Guild.All(Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_SERVER)
        ) {
            if (guild.idLong !in reactMap) reactMap[guild.idLong] = mutableMapOf()
            reactMap[guild.idLong]!!.clear()
            dataManager.write("reacts.json", reactMap)
            channel.sendMessage("Cleared all autoreacts from this server.").queue()
        }

        command("list", "Sends a list of autoreacts for the server to the command invoker.", Access.Guild.All()) {
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
                        .chunkedBy(MessageEmbed.EMBED_MAX_LENGTH_BOT, 25) {
                            it.first.length + it.second.length
                        }
                        .forEach { embeds ->
                            privateChannel.sendEmbed {
                                embeds.forEach { addField(it.first, it.second, false) }
                            }.queue()
                        }
                }, { channel.sendMessage(it.message ?: "An exception was thrown without a message.").queue() })
            } else channel.sendMessage("This server has no autoreacts saved.").queue()
        }
    }

    listener<MessageReceivedEvent> {
        if (channel is TextChannel && guild.idLong in reactMap && author != jda.selfUser) {
            reactMap[guild.idLong]!!
                .filter { it.key in message.contentRaw }
                .values
                .flatten()
                .forEach { message.addReaction(it.emote)?.queue() }
        }
    }
}
