package com.serebit.autotitan.modules

import com.google.gson.Gson
import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.extensions.jda.sendEmbed
import com.serebit.autotitan.api.extensions.limitLengthTo
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.data.DataManager
import com.serebit.autotitan.network.UrbanDictionaryApi
import com.serebit.autotitan.network.WordnikApi
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@Suppress("UNUSED")
class Dictionary : ModuleTemplate(isOptional = true) {
    private val gson = Gson()
    private val dataManager = DataManager(this)

    init {
        command(
            "urban",
            "Gets the Nth Urban Dictionary definition of the given query."
        ) { evt, index: Int, query: LongString -> sendUrbanDefinition(evt, query.value, index) }

        command("urban", "Gets the first Urban Dictionary definition of the given query.") { evt, query: LongString ->
            sendUrbanDefinition(evt, query.value)
        }
    }

    private fun sendWordnikDefinition(evt: MessageReceivedEvent, query: String, index: Int = 1) {
        when {
            !WordnikApi.isInitialized -> evt.channel.sendMessage(
                "Wordnik is not initialized. Initialize it with the command `initwordnik`."
            ).queue()
            WordnikApi.hasDefinitions(query) -> WordnikApi.getDefinition(query, index - 1)?.let { definition ->
                evt.channel.sendEmbed {
                    setTitle(
                        "$query (Definition $index of ${WordnikApi.numDefinitions(query)})",
                        "https://www.wordnik.com/words/$query"
                    )
                    setDescription("*${definition.partOfSpeech}*\n${definition.text}")
                    setFooter("Powered by Wordnik", "http://www.wordnik.com/img/wordnik_gearheart.png")
                }.queue()
            } ?: evt.channel.sendMessage("No definition was found at that index.").queue()
            else -> evt.channel.sendMessage("No definitions were found.").queue()
        }
    }

    private fun sendUrbanDefinition(evt: MessageReceivedEvent, query: String, index: Int = 1) {
        if (UrbanDictionaryApi.hasDefinitions(query)) {
            UrbanDictionaryApi.getDefinition(query, index - 1)?.let { definition ->
                val text = definition
                    .definition
                    .replace("\\[word]".toRegex(), "")
                    .replace("\\[(.+?)]".toRegex(), "$1")
                evt.channel.sendEmbed {
                    setTitle(
                        "$query (Definition $index of ${UrbanDictionaryApi.numDefinitions(query)})",
                        definition.permalink
                    )
                    setDescription(text.limitLengthTo(MessageEmbed.VALUE_MAX_LENGTH))
                    setFooter(
                        "Powered by Urban Dictionary",
                        "https://res.cloudinary.com/hrscywv4p/image/upload/v1/1194347/vo5ge6mdw4creyrgaq2m.png"
                    )
                }.queue()
            } ?: evt.channel.sendMessage("No definition was found at that index.").queue()
        } else evt.channel.sendMessage("No definitions were found.").queue()
    }
}
