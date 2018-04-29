package com.serebit.autotitan.modules

import com.google.gson.Gson
import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.apiwrappers.UrbanDictionaryApi
import com.serebit.autotitan.apiwrappers.WordnikApi
import com.serebit.autotitan.data.DataManager
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.limitLengthTo
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@Suppress("UNUSED")
class Dictionary : ModuleTemplate(isOptional = true) {
    private val gson = Gson()
    private val dataManager = DataManager(this::class)
    private val config = dataManager.readOrDefault("config.json") { DictionaryConfig() }

    init {
        config.apiKey?.let {
            WordnikApi.init(it)
        }

        command(
            "initWordnik",
            "Initializes the module with a Wordnik API key from https://dev.wordnik.com.",
            Access.Private.BotOwner()
        ) { evt, apiKey: String ->
            if (WordnikApi.init(apiKey)) {
                config.apiKey = apiKey
                dataManager.write("config.json", config)
                evt.channel.sendMessage("Wordnik has been initialized.").complete()
            } else {
                evt.channel.sendMessage(
                    "The given API key is invalid. Try again with a valid Wordnik API key."
                ).complete()
            }
        }

        command(
            "define",
            description = "Gets the Nth definition of the given query.",
            delimitLastString = false
        ) { evt, index: Int, query: String -> sendWordnikDefinition(evt, query, index) }

        command(
            "define",
            "Gets the first definition of the given query.",
            delimitLastString = false
        ) { evt, query: String -> sendWordnikDefinition(evt, query) }

        command(
            "related",
            description = "Gets synonyms and antonyms for the given query.",
            delimitLastString = false
        ) { evt, query: String ->
            when {
                !WordnikApi.isInitialized -> evt.channel.sendMessage(
                    "The Dictionary module is not initialized. Initialize it with the command `initdictionary`."
                ).complete()
                WordnikApi.hasRelatedWords(query) -> WordnikApi.getRelatedWords(query)?.let { related ->
                    evt.channel.sendEmbed {
                        setTitle("Words related to $query", "https://www.wordnik.com/words/$query")
                        setDescription(related.joinToString("\n") {
                            // example: "Antonyms: *wet, moisten, soak, water*"
                            "${it.relationshipType.capitalize()}s: *${it.words.joinToString(", ")}*"
                        })
                        setFooter("Powered by Wordnik", "http://www.wordnik.com/img/wordnik_gearheart.png")
                    }.complete()
                }
                else -> evt.channel.sendMessage("No related words were found.").queue()
            }
        }

        command(
            "urban",
            description = "Gets the Nth Urban Dictionary definition of the given query.",
            delimitLastString = false
        ) { evt, index: Int, query: String -> sendUrbanDefinition(evt, query, index) }

        command(
            "urban",
            description = "Gets the first Urban Dictionary definition of the given query.",
            delimitLastString = false
        ) { evt, query: String -> sendUrbanDefinition(evt, query) }
    }

    private fun sendWordnikDefinition(evt: MessageReceivedEvent, query: String, index: Int = 1) {
        when {
            !WordnikApi.isInitialized -> evt.channel.sendMessage(
                "Wordnik is not initialized. Initialize it with the command `initwordnik`."
            ).complete()
            WordnikApi.hasDefinitions(query) -> WordnikApi.getDefinition(query, index - 1)?.let { definition ->
                evt.channel.sendEmbed {
                    setTitle(
                        "$query (Definition $index of ${WordnikApi.numDefinitions(query)})",
                        "https://www.wordnik.com/words/$query"
                    )
                    setDescription("*${definition.partOfSpeech}*\n${definition.text}")
                    setFooter("Powered by Wordnik", "http://www.wordnik.com/img/wordnik_gearheart.png")
                }.complete()
            } ?: evt.channel.sendMessage("No definition was found at that index.").complete()
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

    private data class DictionaryConfig(var apiKey: String? = null)
}
