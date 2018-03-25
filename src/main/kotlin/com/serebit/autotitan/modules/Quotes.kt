package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.data.GuildResourceMap
import com.serebit.extensions.jda.mentionsUsers
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*

@Suppress("UNUSED", "TooManyFunctions")
class Quotes : Module(isOptional = true) {
    private val dataManager = DataManager(this::class)
    private val quoteMap = dataManager.read("quotes.json") ?: GuildResourceMap<String, String>()
    private val random = Random()

    @Command(
        description = "Adds the given quote.",
        locale = Locale.GUILD,
        splitLastParameter = false
    )
    fun addQuote(evt: MessageReceivedEvent, quote: String) {
        if (evt.message.mentionsUsers) {
            evt.channel.sendMessage("Quotes containing mentions are not permitted.").complete()
            return
        }
        quoteMap[evt.guild].let {
            val quoteIndex = it.keys.map { it.toInt() }.max()?.plus(1) ?: 0
            it[quoteIndex.toString()] = quote
            evt.channel.sendMessage("Added ${evt.member.asMention}'s quote as number `$quoteIndex`.").complete()
        }
        dataManager.write("quotes.json", quoteMap)
    }

    @Command(
        description = "Deletes the quote at the given index.",
        locale = Locale.GUILD
    )
    fun deleteQuote(evt: MessageReceivedEvent, index: Int) {
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

    @Command(
        description = "Gets a random quote, if any exist.",
        locale = Locale.GUILD
    )
    fun quote(evt: MessageReceivedEvent) {
        val quotes = quoteMap[evt.guild]

        if (quotes.isNotEmpty()) {
            val quote = quotes.filter { it.value.isNotBlank() }.let {
                it.values.toList()[random.nextInt(it.size)]
            }
            evt.channel.sendMessage(quote).complete()
        } else evt.channel.sendMessage("This server has no quotes saved.").complete()
    }

    @Command(description = "Gets the quote at the given index.", locale = Locale.GUILD)
    fun quote(evt: MessageReceivedEvent, index: Int) {
        val quotes = quoteMap[evt.guild]

        if (quotes.isNotEmpty()) {
            quotes[index.toString()]?.let { quote ->
                evt.channel.sendMessage(quote).complete()
            } ?: evt.channel.sendMessage("There is no quote with an index of `$index`.").complete()
        } else evt.channel.sendMessage("This server has no quotes saved.").complete()
    }

    @Command(description = "Gets the list of quotes that this server has saved.", locale = Locale.GUILD)
    fun quoteList(evt: MessageReceivedEvent) {
        val quotes = quoteMap[evt.guild]

        if (quotes.isNotEmpty()) {
            val file = createTempFile(prefix = "quotes", suffix = ".txt").also {
                it.writeText(quotes.entries.joinToString("\n\n") { (key, value) ->
                    "$key: $value"
                })
            }
            evt.channel.sendFile(file).queue {
                file.delete()
            }
        } else evt.channel.sendMessage("This server has no quotes saved.").complete()
    }
}
