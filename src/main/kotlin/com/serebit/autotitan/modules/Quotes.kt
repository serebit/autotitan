package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*

@Suppress("UNUSED", "TooManyFunctions")
class Quotes : Module(isOptional = true) {
    private val dataManager = DataManager(this::class)
    private val quoteMap = dataManager.read("quotes.json") ?: QuoteMap()
    private val random = Random()

    @Command(
        description = "Adds the given quote.",
        locale = Locale.GUILD,
        splitLastParameter = false
    )
    fun addQuote(evt: MessageReceivedEvent, quote: String) {
        evt.message.run {
            if (mentionedUsers.isNotEmpty() || mentionedRoles.isNotEmpty() || mentionsEveryone()) {
                channel.sendMessage("Quotes containing mentions are not permitted.").complete()
                return
            }
        }
        quoteMap.getOrPutDefault(evt.guild.idLong).let {
            val quoteIndex = it.keys.max()?.plus(1) ?: 0
            it[quoteIndex] = quote
            evt.channel.sendMessage("Added ${evt.member.asMention}'s quote as number `$quoteIndex`.").complete()
        }
        dataManager.write("quotes.json", quoteMap)
    }

    @Command(
        description = "Deletes the quote at the given index.",
        locale = Locale.GUILD
    )
    fun deleteQuote(evt: MessageReceivedEvent, index: Int) {
        quoteMap.getOrElse(evt.guild.idLong) {
            evt.channel.sendMessage("There are no quotes to delete.").complete()
            return
        }.let {
                if (!it.contains(index)) {
                    evt.channel.sendMessage("A quote with that number cannot be found.").complete()
                    return
                }
                it[index] = ""
                evt.channel.sendMessage("Removed quote `$index`.").complete()
            }
        dataManager.write("quotes.json", quoteMap)
    }

    @Command(
        description = "Gets a random quote, if any exist.",
        locale = Locale.GUILD
    )
    fun quote(evt: MessageReceivedEvent) {
        quoteMap.getOrElse(evt.guild.idLong) {
            evt.channel.sendMessage("No quotes are available.").complete()
            return
        }.let { map ->
                if (map.isEmpty() || map.all { it.value.isBlank() }) {
                    evt.channel.sendMessage("No quotes are available.").complete()
                    return
                }
                val quote = map.filter { it.value.isNotBlank() }.let {
                    it.values.toList()[random.nextInt(it.size)]
                }
                evt.channel.sendMessage(quote).complete()
            }
    }

    @Command(description = "Gets the quote at the given index.", locale = Locale.GUILD)
    fun quote(evt: MessageReceivedEvent, index: Int) {
        quoteMap[evt.guild.idLong]?.let { quotes ->
            val quote = quotes[index]
            when {
                quotes.isEmpty() || quotes.all { it.value.isBlank() } -> {
                    evt.channel.sendMessage("No quotes are available.").complete()
                }
                quote.isNullOrBlank() -> {
                    evt.channel.sendMessage("A quote with that index could not be found.").complete()
                }
                else -> evt.channel.sendMessage(quote).complete()
            }
        } ?: evt.channel.sendMessage("No quotes are available.").complete()
    }

    private class QuoteMap : MutableMap<Long, MutableMap<Int, String>> by mutableMapOf() {
        fun getOrPutDefault(key: Long): MutableMap<Int, String> = getOrPut(key, { mutableMapOf() })
    }
}
