import com.serebit.autotitan.api.command
import com.serebit.autotitan.api.extensions.*
import com.serebit.autotitan.api.group
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.data.GuildResourceList
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.MessageEmbed

fun String.trimWhitespace(): String = replace("(\\s){2,}".toRegex(), "$1$1")

module("Quotes", isOptional = true, defaultAccess = Access.Guild.All()) {
    val quoteMap = dataManager.readOrDefault("quotes.json") { GuildResourceList<String?>() }

    group("quote") {
        command("add", "Adds the given quote.") { quote: LongString ->
            if (guild.idLong !in quoteMap) quoteMap[guild.idLong] = mutableListOf()
            if (message.mentionsUsers) {
                channel.sendMessage("Quotes containing mentions are not permitted.").queue()
            } else {
                quoteMap[guild.idLong]!!.apply {
                    add(quote.value)
                    channel.sendMessage("Added ${member.asMention}'s quote as number `${size - 1}`.").queue()
                }
                dataManager.write("quotes.json", quoteMap)
            }
        }

        command("remove", "Deletes the quote at the given index.") { index: Int ->
            if (guild.idLong !in quoteMap) quoteMap[guild.idLong] = mutableListOf()
            val quotes = quoteMap[guild.idLong]!!

            when {
                quotes.isEmpty() -> channel.sendMessage("This server has no quotes saved.").queue()
                index !in quotes.indices -> channel.sendMessage("There is no quote with an index of `$index`.").queue()
                else -> {
                    quotes[index] = null
                    dataManager.write("quotes.json", quoteMap)
                    channel.sendMessage("Removed quote `$index`.").queue()
                }
            }
        }

        command("list", "Gets the list of quotes that this server has saved.") {
            if (guild.idLong !in quoteMap) quoteMap[guild.idLong] = mutableListOf()
            if (quoteMap[guild.idLong]!!.isEmpty()) {
                channel.sendMessage("This server has no quotes saved.").queue()
            } else {
                channel.sendMessage("Sending a quote list in PMs.").queue()
                author.openPrivateChannel().queue({ privateChannel ->
                    quoteMap[guild.idLong]!!.filterNotNull().mapIndexed { index, quote ->
                        index.toString() to quote.trimWhitespace().limitLengthTo(MessageEmbed.VALUE_MAX_LENGTH)
                    }.chunkedBy(MessageEmbed.EMBED_MAX_LENGTH_BOT, MESSAGE_EMBED_MAX_FIELDS) {
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
            if (guild.idLong !in quoteMap) quoteMap[guild.idLong] = mutableListOf()
            if (quoteMap[guild.idLong]!!.isEmpty()) {
                channel.sendMessage("This server has no quotes to shuffle.").queue()
            } else {
                val emptyQuotes = quoteMap[guild.idLong]!!.count { it == null }
                quoteMap[guild.idLong]!!.removeAll { it == null }
                dataManager.write("quotes.json", quoteMap)
                channel.sendMessage("Shuffled this server's quotes. $emptyQuotes indices were removed.").queue()
            }
        }
    }

    command("quote", "Gets a random quote, if any exist.") {
        if (guild.idLong !in quoteMap) quoteMap[guild.idLong] = mutableListOf()
        if (quoteMap[guild.idLong]!!.isNotEmpty()) {
            channel.sendMessage(quoteMap[guild.idLong]!!.filterNotNull().random()).queue()
        } else channel.sendMessage("This server has no quotes saved.").queue()
    }

    command("quote", "Gets the quote at the given index.") { index: Int ->
        if (guild.idLong !in quoteMap) quoteMap[guild.idLong] = mutableListOf()
        if (quoteMap[guild.idLong]!!.isEmpty()) {
            channel.sendMessage("This server has no quotes saved.").queue()
        } else {
            quoteMap[guild.idLong]!![index]?.let { channel.sendMessage(it).queue() }
                ?: channel.sendMessage("There is no quote with an index of `$index`.").queue()
        }
    }
}
