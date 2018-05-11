package com.serebit.autotitan.modules

import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.data.GuildResourceMap
import com.serebit.extensions.chunkedBy
import com.serebit.extensions.jda.MESSAGE_EMBED_MAX_FIELDS
import com.serebit.extensions.jda.mentionsUsers
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.limitLengthTo
import com.serebit.extensions.trimWhitespace
import net.dv8tion.jda.core.entities.MessageEmbed
import java.util.*

@Suppress("UNUSED")
class Quotes : ModuleTemplate(isOptional = true, defaultAccess = Access.Guild.All()) {
    private val dataManager = DataManager(this)
    private val quoteMap = dataManager.readOrDefault("quotes.json") { GuildResourceMap<String, String>() }
    private val random = Random()

    init {
        command("addQuote", "Adds the given quote.") { evt, quote: LongString ->
            if (evt.message.mentionsUsers) {
                evt.channel.sendMessage("Quotes containing mentions are not permitted.").queue()
                return@command
            }
            quoteMap[evt.guild].let {
                val quoteIndex = it.keys.map { it.toInt() }.max()?.plus(1) ?: 0
                it[quoteIndex.toString()] = quote.value
                evt.channel.sendMessage("Added ${evt.member.asMention}'s quote as number `$quoteIndex`.").queue()
            }
            dataManager.write("quotes.json", quoteMap)
        }

        command("deleteQuote", "Deletes the quote at the given index.") { evt, index: Int ->
            val quotes = quoteMap[evt.guild]

            when {
                quotes.isEmpty() -> evt.channel.sendMessage("This server has no quotes saved.").queue()
                index.toString() !in quotes -> {
                    evt.channel.sendMessage("There is no quote with an index of `$index`.").queue()
                }
                else -> {
                    quotes.remove(index.toString())
                    dataManager.write("quotes.json", quoteMap)
                    evt.channel.sendMessage("Removed quote `$index`.").queue()
                }
            }
        }

        command("quote", "Gets a random quote, if any exist.") { evt ->
            val quotes = quoteMap[evt.guild]

            if (quotes.isNotEmpty()) {
                val quote = quotes.filter { it.value.isNotBlank() }.let {
                    it.values.toList()[random.nextInt(it.size)]
                }
                evt.channel.sendMessage(quote).queue()
            } else evt.channel.sendMessage("This server has no quotes saved.").queue()
        }

        command("quote", "Gets the quote at the given index.") { evt, index: Int ->
            val quotes = quoteMap[evt.guild]

            if (quotes.isNotEmpty()) {
                quotes[index.toString()]?.let { quote ->
                    evt.channel.sendMessage(quote).queue()
                } ?: evt.channel.sendMessage("There is no quote with an index of `$index`.").queue()
            } else evt.channel.sendMessage("This server has no quotes saved.").queue()
        }

        command("quoteList", "Gets the list of quotes that this server has saved.") { evt ->
            val quotes = quoteMap[evt.guild]
            if (quotes.isNotEmpty()) {
                evt.channel.sendMessage("Sending a quote list in PMs.").queue()
                evt.author.openPrivateChannel().queue({ privateChannel ->
                    quotes
                        .map { (index, quote) ->
                            index.limitLengthTo(MessageEmbed.TITLE_MAX_LENGTH) to
                                quote.trimWhitespace().limitLengthTo(MessageEmbed.VALUE_MAX_LENGTH)
                        }
                        .chunkedBy(MessageEmbed.EMBED_MAX_LENGTH_BOT, MESSAGE_EMBED_MAX_FIELDS) {
                            it.first.length + it.second.length
                        }
                        .forEach { embeds ->
                            privateChannel.sendEmbed {
                                embeds.forEach { addField(it.first, it.second, false) }
                            }.queue()
                        }
                }, {
                    evt.channel.sendMessage(
                        "Failed to send the quote list. Make sure that you haven't blocked me!"
                    ).queue()
                })
            } else evt.channel.sendMessage("This server has no quotes saved.").queue()
        }
    }
}
