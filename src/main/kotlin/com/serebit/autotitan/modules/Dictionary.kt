package com.serebit.autotitan.modules

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Restrictions
import com.serebit.autotitan.apiwrappers.WordnikApi
import com.serebit.autotitan.data.DataManager
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.limitLengthTo
import khttp.get
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.net.URLEncoder

@Suppress("UNUSED", "TooManyFunctions")
class Dictionary : Module(isOptional = true) {
    private val gson = Gson()
    private val dataManager = DataManager(this::class)
    private val config = dataManager.read("config.json") ?: DictionaryConfig()

    init {
        config.apiKey?.let {
            WordnikApi.init(it)
        }

        command(
            "initDictionary",
            "Initializes the module with a Wordnik API key from https://dev.wordnik.com.",
            Restrictions(Access.BotOwner)
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
            description = "Gets the first Urban Dictionary definition of the given query.",
            delimitLastString = false
        ) { evt, query: String -> getUrbanDefinition(evt, 0, query) }

        command(
            "urban",
            description = "Gets the Nth Urban Dictionary definition of the given query.",
            delimitLastString = false,
            task = this::getUrbanDefinition
        )
    }

    private fun getUrbanDefinition(evt: MessageReceivedEvent, index: Int, query: String) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val apiResult = get("https://api.urbandictionary.com/v0/define?term=$encodedQuery")
        if (apiResult.jsonObject["result_type"] != "no_results") {
            val definitions = gson.fromJson<UrbanDictionaryResult>(apiResult.text).list
            val definition = definitions[index - 1]
                .definition
                .replace("\\[word]".toRegex(), "")
                .replace("\\[(.+?)]".toRegex(), "$1")
            evt.channel.sendEmbed {
                setTitle("$query (Definition $index of ${definitions.size})", definitions[0].permalink)
                setDescription(definition.limitLengthTo(MessageEmbed.VALUE_MAX_LENGTH))
                setFooter(
                    "Powered by Urban Dictionary",
                    "https://res.cloudinary.com/hrscywv4p/image/upload/v1/1194347/vo5ge6mdw4creyrgaq2m.png"
                )
            }.complete()
        } else evt.channel.sendMessage("Couldn't find a definition at that index.").complete()
    }

    private data class DictionaryConfig(var apiKey: String? = null)

    private data class UrbanDictionaryResult(val list: List<UrbanDictionaryDefinition>)

    private data class UrbanDictionaryDefinition(val definition: String, val permalink: String, val example: String)
}
