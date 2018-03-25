package com.serebit.autotitan.modules

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.data.DataManager
import com.serebit.extensions.jda.sendEmbed
import khttp.get
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.jeremybrooks.knicker.AccountApi
import net.jeremybrooks.knicker.WordApi
import net.jeremybrooks.knicker.dto.Definition
import net.jeremybrooks.knicker.dto.Related
import net.jeremybrooks.knicker.dto.TokenStatus
import java.net.URLEncoder

@Suppress("UNUSED", "TooManyFunctions")
class Dictionary : Module(isOptional = true) {
    private val gson = Gson()
    private val dataManager = DataManager(this::class)
    private val config = dataManager.read("config.json") ?: WordnikConfig()
    private val definitionCache: MutableMap<String, List<Definition>> = mutableMapOf()
    private val relatedWordsCache: MutableMap<String, List<Related>> = mutableMapOf()

    init {
        config.apiKey?.let {
            System.setProperty("WORDNIK_API_KEY", config.apiKey)
        }
    }

    @Command(
        description = "Initializes the module with a Wordnik API key from https://dev.wordnik.com.",
        access = Access.BOT_OWNER
    )
    fun initDictionary(evt: MessageReceivedEvent, apiKey: String) {
        System.setProperty("WORDNIK_API_KEY", apiKey)
        if (AccountApi.apiTokenStatus().isValid) {
            config.apiKey = apiKey
            dataManager.write("config.json", config)
            if (evt.textChannel != null) evt.message.delete().complete()
            evt.channel.sendMessage("Dictionary module has been initialized.").complete()
        } else {
            System.clearProperty("WORDNIK_API_KEY")
            if (evt.textChannel != null) evt.message.delete().complete()
            evt.channel.sendMessage("The given API key is invalid. Try again, with a valid API key.").complete()
        }
    }

    @Command(description = "Gets the first definition of the given query.", splitLastParameter = false)
    fun define(evt: MessageReceivedEvent, wordOrPhrase: String) {
        if (AccountApi.apiTokenStatus().isInvalid) {
            evt.channel.sendMessage(
                "The Dictionary module is not initialized. Initialize it with the command `initdictionary`."
            ).complete()
            return
        }

        val definitions = definitionCache.getOrPut(wordOrPhrase) {
            WordApi.definitions(wordOrPhrase)
        }

        if (definitions.isEmpty()) {
            evt.channel.sendMessage(
                "No definitions were found for `$wordOrPhrase`. Make sure it's spelled correctly."
            ).complete()
            return
        }

        evt.channel.sendEmbed {
            definitions[0].let {
                setTitle(
                    "${it.word} (Definition 1 of ${definitions.size})",
                    "https://www.wordnik.com/words/$wordOrPhrase"
                )
                setDescription("*${it.partOfSpeech}*\n${it.text}")
            }
            setFooter("Powered by Wordnik", "http://www.wordnik.com/img/wordnik_gearheart.png")
        }.complete()
    }

    @Command(description = "Gets the Nth definition of the given query.", splitLastParameter = false)
    fun define(evt: MessageReceivedEvent, index: Int, wordOrPhrase: String) {
        if (AccountApi.apiTokenStatus().isInvalid) {
            evt.channel.sendMessage(
                "The Dictionary module is not initialized. Initialize it with the command `initdictionary`."
            ).complete()
            return
        }

        val definitions = definitionCache.getOrPut(wordOrPhrase) {
            WordApi.definitions(wordOrPhrase)
        }

        when {
            definitions.isEmpty() -> evt.channel.sendMessage(
                "No definitions were found for `$wordOrPhrase`. Make sure it's spelled correctly."
            ).complete()
            index - 1 !in definitions.indices -> evt.channel.sendMessage(
                "There is no definition for `$wordOrPhrase` with the given index."
            ).complete()
            else -> evt.channel.sendEmbed {
                definitions[index - 1].let {
                    setTitle(
                        "${it.word} (Definition $index of ${definitions.size})",
                        "https://www.wordnik.com/words/$wordOrPhrase"
                    )
                    setDescription("*${it.partOfSpeech}*\n${it.text}")
                }
                setFooter("Powered by Wordnik", "http://www.wordnik.com/img/wordnik_gearheart.png")
            }.complete()
        }
    }

    @Command(description = "Gets synonyms and antonyms for the given query.", splitLastParameter = false)
    fun related(evt: MessageReceivedEvent, wordOrPhrase: String) {
        if (AccountApi.apiTokenStatus().isInvalid) {
            evt.channel.sendMessage(
                "The Dictionary module is not initialized. Initialize it with the command `initdictionary`."
            ).complete()
            return
        }

        val related = relatedWordsCache.getOrPut(wordOrPhrase) {
            WordApi.related(wordOrPhrase)
                .filter { it.relType in setOf("synonym", "antonym") }
        }.map { it.relType to it.words }

        if (related.isEmpty()) {
            evt.channel.sendMessage(
                "No words related to `$wordOrPhrase` were found. Make sure it's spelled correctly."
            ).complete()
            return
        }

        evt.channel.sendEmbed {
            setTitle("Words related to $wordOrPhrase", "https://www.wordnik.com/words/$wordOrPhrase")
            setDescription(related.sortedByDescending { it.first }.joinToString("\n") {
                // example: "Antonyms: *wet, moisten, soak, water*"
                "${it.first.capitalize()}s: *${it.second.joinToString(", ")}*"
            })
            setFooter("Powered by Wordnik", "http://www.wordnik.com/img/wordnik_gearheart.png")
        }.complete()
    }

    @Command(description = "Gets the first Urban Dictionary definition of the given query.", splitLastParameter = false)
    fun urban(evt: MessageReceivedEvent, query: String) = urban(evt, 1, query)

    @Command(description = "Gets the Nth Urban Dictionary definition of the given query.", splitLastParameter = false)
    fun urban(evt: MessageReceivedEvent, index: Int, query: String) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val apiResult = get("https://api.urbandictionary.com/v0/define?term=$encodedQuery")
        when {
            apiResult.jsonObject["result_type"] != "no_results" -> {
                val definitions = gson.fromJson<UrbanDictionaryResult>(apiResult.text).list
                val definition = definitions[index - 1]
                    .definition
                    .replace("\\[word]".toRegex(), "")
                    .replace(
                        "\\[(.+?)]".toRegex(),
                        "$1"
                    )
                val trimmedDefinition = if (definition.length > maxEmbedFieldLength) {
                    definition.substring(0 until maxEmbedFieldLength) + '\u2026'
                } else definition
                evt.channel.sendEmbed {
                    setTitle("$query (Definition 1 of ${definitions.size})", definitions[0].permalink)
                    setDescription(trimmedDefinition)
                    setFooter(
                        "Powered by Urban Dictionary",
                        "https://res.cloudinary.com/hrscywv4p/image/upload/v1/1194347/vo5ge6mdw4creyrgaq2m.png"
                    )
                }.complete()
            }
            index == 1 -> evt.channel.sendMessage("Couldn't find a definition. Maybe you spelled it wrong?").complete()
            else -> evt.channel.sendMessage("Couldn't find a definition at that index.").complete()
        }
    }

    private data class WordnikConfig(var apiKey: String? = null)

    private data class UrbanDictionaryResult(
        val list: List<UrbanDictionaryDefinition>
    )

    private data class UrbanDictionaryDefinition(val definition: String, val permalink: String, val example: String)

    private val TokenStatus.isInvalid get() = !isValid

    companion object {
        private const val maxEmbedFieldLength = 1024
    }
}
