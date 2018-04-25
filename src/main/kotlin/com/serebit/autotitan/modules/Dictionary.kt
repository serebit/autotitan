package com.serebit.autotitan.modules

import com.google.gson.Gson
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.apiwrappers.UrbanDictionaryApi
import com.serebit.autotitan.apiwrappers.WordnikApi
import com.serebit.autotitan.data.DataManager
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.limitLengthTo
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@Suppress("UNUSED")
class Dictionary : Module(isOptional = true) {
    private val gson = Gson()
    private val dataManager = DataManager(this::class)
    private val config = dataManager.readOrDefault("config.json") { DictionaryConfig() }

    init {
        config.apiKey?.let {
            WordnikApi.init(it)
        }

        command(
            "initDictionary",
            "Initializes the module with a Wordnik API key from https://dev.wordnik.com.",
            Access.BotOwner()
        ) { evt, apiKey: String ->
            if (WordnikApi.init(apiKey)) {
                config.apiKey = apiKey
                dataManager.write("config.json", config)
                if (evt.textChannel != null) evt.message.delete().complete()
                evt.channel.sendMessage("Dictionary module has been initialized.").complete()
            } else {
                if (evt.textChannel != null) evt.message.delete().complete()
                evt.channel.sendMessage("The given API key is invalid. Try again, with a valid API key.").complete()
            }
        }

        command(
            "define",
            description = "Gets the Nth definition of the given query.",
            delimitLastString = false
        ) { evt, index: Int, wordOrPhrase: String ->
            when {
                !WordnikApi.isInitialized -> evt.channel.sendMessage(
                    "The Dictionary module is not initialized. Initialize it with the command `initdictionary`."
                ).complete()
                WordnikApi.hasDefinitions(wordOrPhrase) -> WordnikApi.getDefinition(wordOrPhrase, index - 1)?.let {
                    evt.channel.sendEmbed {
                        setTitle(
                            "$wordOrPhrase (Definition $index of ${WordnikApi.numDefinitions(wordOrPhrase)})",
                            "https://www.wordnik.com/words/$wordOrPhrase"
                        )
                        setDescription("*${it.partOfSpeech}*\n${it.text}")
                        setFooter("Powered by Wordnik", "http://www.wordnik.com/img/wordnik_gearheart.png")
                    }.complete()
                } ?: evt.channel.sendMessage("No definition was found at that index.").complete()
                else -> evt.channel.sendMessage("No definitions were found.").queue()
            }
        }

        command(
            "define",
            "Gets the first definition of the given query.",
            delimitLastString = false
        ) { evt, wordOrPhrase: String ->
            when {
                !WordnikApi.isInitialized -> evt.channel.sendMessage(
                    "The Dictionary module is not initialized. Initialize it with the command `initdictionary`."
                ).complete()
                WordnikApi.hasDefinitions(wordOrPhrase) -> WordnikApi.getDefinition(wordOrPhrase)?.let { definition ->
                    evt.channel.sendEmbed {
                        setTitle(
                            "$wordOrPhrase (Definition 1 of ${WordnikApi.numDefinitions(wordOrPhrase)})",
                            "https://www.wordnik.com/words/$wordOrPhrase"
                        )
                        setDescription("*${definition.partOfSpeech}*\n${definition.text}")
                        setFooter("Powered by Wordnik", "http://www.wordnik.com/img/wordnik_gearheart.png")
                    }.complete()
                }
                else -> evt.channel.sendMessage("No definitions were found.").queue()
            }
        }


        command(
            "related",
            description = "Gets synonyms and antonyms for the given query.",
            delimitLastString = false
        ) { evt, wordOrPhrase: String ->
            when {
                !WordnikApi.isInitialized -> evt.channel.sendMessage(
                    "The Dictionary module is not initialized. Initialize it with the command `initdictionary`."
                ).complete()
                WordnikApi.hasRelatedWords(wordOrPhrase) -> WordnikApi.getRelatedWords(wordOrPhrase)?.let { related ->
                    evt.channel.sendEmbed {
                        setTitle("Words related to $wordOrPhrase", "https://www.wordnik.com/words/$wordOrPhrase")
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
