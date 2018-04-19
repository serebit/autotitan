package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Restrictions
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.data.GuildResourceMap
import com.serebit.extensions.jda.MESSAGE_EMBED_MAX_FIELDS
import com.serebit.extensions.chunkedBy
import com.serebit.extensions.jda.mentionsUsers
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.limitLengthTo
import net.dv8tion.jda.core.entities.MessageEmbed
import java.util.*

@Suppress("UNUSED", "TooManyFunctions")
class Quotes : Module(isOptional = true) {
    private val dataManager = DataManager(this::class)
    private val quoteMap = dataManager.read("quotes.json") ?: GuildResourceMap<String, String>()
    private val random = Random()

    init {
        command(
            "addQuote",
            "Adds the given quote.",
            Restrictions(Access.Guild.All),
            delimitLastString = false
        ) { evt, quote: String ->
            if (evt.message.mentionsUsers) {
                evt.channel.sendMessage("Quotes containing mentions are not permitted.").complete()
                return@command
            }
            quoteMap[evt.guild].let {
                val quoteIndex = it.keys.map { it.toInt() }.max()?.plus(1) ?: 0
                it[quoteIndex.toString()] = quote
                evt.channel.sendMessage("Added ${evt.member.asMention}'s quote as number `$quoteIndex`.").complete()
            }
            dataManager.write("quotes.json", quoteMap)
        }

        command(
            "deleteQuote",
            "Deletes the quote at the given index.",
            Restrictions(Access.Guild.All)
        ) { evt, index: Int ->
            val quotes = quoteMap[evt.guild]

            when {
                quotes.isEmpty() -> evt.channel.sendMessage("This server has no quotes saved.").complete()
                index.toString() !in quotes -> {
                    evt.channel.sendMessage("There is no quote with an index of `$index`.").complete()
                }
                else -> {
                    quotes.remove(index.toString())
                    dataManager.write("quotes.json", quoteMap)
                    evt.channel.sendMessage("Removed quote `$index`.").complete()
                }
            }
        }

        command(
            "quote",
            "Gets a random quote, if any exist.",
            Restrictions(Access.Guild.All)
        ) { evt ->
            val quotes = quoteMap[evt.guild]

            if (quotes.isNotEmpty()) {
                val quote = quotes.filter { it.value.isNotBlank() }.let {
                    it.values.toList()[random.nextInt(it.size)]
                }
                evt.channel.sendMessage(quote).complete()
            } else evt.channel.sendMessage("This server has no quotes saved.").complete()
        }

        command(
            "quote",
            "Gets the quote at the given index.",
            Restrictions(Access.Guild.All)
        ) { evt, index: Int ->
            val quotes = quoteMap[evt.guild]

            if (quotes.isNotEmpty()) {
                quotes[index.toString()]?.let { quote ->
                    evt.channel.sendMessage(quote).complete()
                } ?: evt.channel.sendMessage("There is no quote with an index of `$index`.").complete()
            } else evt.channel.sendMessage("This server has no quotes saved.").complete()
        }

        command(
            "quoteList",
            "Gets the list of quotes that this server has saved.",
            Restrictions(Access.Guild.All)
        ) { evt ->
            val quotes = quoteMap[evt.guild]
            if (quotes.isNotEmpty()) {
                evt.channel.sendMessage("Sending a quote list in PMs.").complete()
                evt.author.openPrivateChannel().queue({ privateChannel ->
                    quotes
                        .map { (index, quote) ->
                            index.limitLengthTo(MessageEmbed.TITLE_MAX_LENGTH) to
                                quote.limitLengthTo(MessageEmbed.VALUE_MAX_LENGTH)
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
                    ).complete()
                })
            } else evt.channel.sendMessage("This server has no quotes saved.").complete()
        }
    }
}
