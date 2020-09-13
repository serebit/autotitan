import com.serebit.autotitan.api.*
import com.serebit.autotitan.extensions.chunkedBy
import com.serebit.autotitan.extensions.limitLengthTo
import com.serebit.autotitan.extensions.sendEmbed
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed

fun String.trimWhitespace(): String = replace("(\\s){2,}".toRegex(), "$1$1")

val Message.mentionsUsers get() = mentionedUsers.isNotEmpty() || mentionedMembers.isNotEmpty() || mentionsEveryone()

optionalModule("Quotes", defaultAccess = Access.Guild.All()) {
    val quoteMap = dataManager.readOrDefault("quotes.json") { GuildResourceMap<Int, String>() }
        .withDefault { mutableMapOf() }

    group("quote") {
        command("add", "Adds the given quote.") { quote: LongString ->
            if (message.mentionsUsers) {
                channel.sendMessage("Quotes containing mentions are not permitted.").queue()
            } else {
                quoteMap[guild.idLong]!!.apply {
                    set(size - 1, quote.value)
                    channel.sendMessage("Added ${member!!.asMention}'s quote as number `${size - 1}`.").queue()
                }
                dataManager.write("quotes.json", quoteMap)
            }
        }

        command("remove", "Deletes the quote at the given index.") { index: Int ->
            val quotes = quoteMap[guild.idLong]!!

            when {
                quotes.isEmpty() -> channel.sendMessage("This server has no quotes saved.").queue()
                index !in quotes.keys -> channel.sendMessage("There is no quote with an index of `$index`.").queue()
                else -> {
                    quotes.remove(index)
                    dataManager.write("quotes.json", quoteMap)
                    channel.sendMessage("Removed quote `$index`.").queue()
                }
            }
        }

        command("list", "Gets the list of quotes that this server has saved.") {
            if (quoteMap[guild.idLong].isNullOrEmpty()) {
                channel.sendMessage("This server has no quotes saved.").queue()
            } else {
                channel.sendMessage("Sending a quote list in PMs.").queue()
                author.openPrivateChannel().queue({ privateChannel ->
                    quoteMap[guild.idLong]!!.map { (index, quote) ->
                        index.toString() to quote.trimWhitespace().limitLengthTo(MessageEmbed.VALUE_MAX_LENGTH)
                    }.chunkedBy(MessageEmbed.EMBED_MAX_LENGTH_BOT, 25) {
                        it.first.length + it.second.length
                    }.forEach { embeds ->
                        privateChannel.sendEmbed {
                            embeds.forEach { addField(it.first, it.second, false) }
                        }.queue()
                    }
                }, {
                    channel.sendMessage("Failed to send the quote list. Make sure that you haven't blocked me!").queue()
                })
            }
        }

        command(
            "shuffle",
            "Removes the empty quote indices for the given server.",
            access = Access.Guild.All(Permission.MANAGE_CHANNEL)
        ) {
            if (quoteMap[guild.idLong].isNullOrEmpty()) {
                channel.sendMessage("This server has no quotes to shuffle.").queue()
            } else {
                val tempMap = quoteMap[guild.idLong]!!.toMap()
                tempMap.entries.forEachIndexed { index, (key, value) ->
                    quoteMap[guild.idLong]!![index] = value
                }
                val numRemovedIndices = tempMap.keys.last() - quoteMap[guild.idLong]!!.keys.last()
                dataManager.write("quotes.json", quoteMap)
                channel.sendMessage("Shuffled this server's quotes. $numRemovedIndices indices were removed.").queue()
            }
        }
    }

    command("quote", "Gets a random quote, if any exist.") {
        if (quoteMap[guild.idLong]!!.isNotEmpty()) {
            channel.sendMessage(quoteMap[guild.idLong]!!.values.random()).queue()
        } else channel.sendMessage("This server has no quotes saved.").queue()
    }

    command("quote", "Gets the quote at the given index.") { index: Int ->
        if (guild.idLong !in quoteMap) quoteMap[guild.idLong] = mutableMapOf()
        if (quoteMap[guild.idLong]!!.isEmpty()) {
            channel.sendMessage("This server has no quotes saved.").queue()
        } else {
            quoteMap[guild.idLong]!![index]?.let { channel.sendMessage(it).queue() }
                ?: channel.sendMessage("There is no quote with an index of `$index`.").queue()
        }
    }
}
