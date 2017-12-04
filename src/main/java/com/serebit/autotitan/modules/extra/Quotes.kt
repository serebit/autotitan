package com.serebit.autotitan.modules.extra

import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.api.meta.annotations.Module
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*

@Module
class Quotes {
    private val dataManager = DataManager(this::class.java)
    private val quoteMap: QuoteMap = dataManager.read("quotes.json") ?: QuoteMap()
    private val random = Random()

    @Command(locale = Locale.GUILD, delimitFinalParameter = false)
    fun addQuote(evt: MessageReceivedEvent, quote: String) {
        evt.message.run {
            if (mentionedUsers.isNotEmpty() || mentionedRoles.isNotEmpty() || mentionsEveryone()) {
                channel.sendMessage("Quotes containing mentions are not permitted.").complete()
                return
            }
        }
        quoteMap.getOrPutDefault(evt.guild.idLong).let {
            val quoteIndex = it.keys.max()?.plus(1) ?: 0
            it.put(quoteIndex, quote)
            evt.channel.sendMessage("Added ${evt.member.asMention}'s quote as number `$quoteIndex`.").complete()
        }
        dataManager.write("quotes.json", quoteMap)
    }

    @Command(locale = Locale.GUILD)
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

    @Command(locale = Locale.GUILD)
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

    @Command(locale = Locale.GUILD)
    fun getQuote(evt: MessageReceivedEvent, index: Int) {
        quoteMap.getOrElse(evt.guild.idLong) {
            evt.channel.sendMessage("No quotes are available.").complete()
            return
        }.let { map ->
            if (map.isEmpty() || map.all { it.value.isBlank() }) {
                evt.channel.sendMessage("No quotes are available.").complete()
                return
            }
            val quote = map[index]
            if (quote.isNullOrBlank()) {
                evt.channel.sendMessage("A quote with that number could not be found.").complete()
                return
            }
            evt.channel.sendMessage(quote).complete()
        }
    }
}

private class QuoteMap : MutableMap<Long, MutableMap<Int, String>> by mutableMapOf() {
    fun getOrPutDefault(key: Long): MutableMap<Int, String> = getOrPut(key, { mutableMapOf() })
}