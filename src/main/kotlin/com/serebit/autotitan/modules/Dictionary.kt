package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.data.DataManager
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.jeremybrooks.knicker.AccountApi
import net.jeremybrooks.knicker.WordApi
import net.jeremybrooks.knicker.dto.Definition
import net.jeremybrooks.knicker.dto.Related
import net.jeremybrooks.knicker.dto.TokenStatus

@Suppress("UNUSED")
class Dictionary : Module(isOptional = true) {
    private val dataManager = DataManager(this::class.java)
    private val config = dataManager.read("config.json") ?: WordnikConfig()
    private val definitionCache: MutableMap<String, List<Definition>> = mutableMapOf()
    private val relatedWordsCache: MutableMap<String, List<Related>> = mutableMapOf()

    init {
        if (config.apiKey != null) System.setProperty("WORDNIK_API_KEY", config.apiKey)
    }

    @Command(
        description = "Initializes the module. The only argument is a Wordnik API key, which can be obtained on https://www.wordnik.com.",
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

    @Command(description = "Gets the first definition of the given word or phrase.", splitLastParameter = false)
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

    @Command(description = "Gets the Nth definition of the given word or phrase.", splitLastParameter = false)
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

        if (definitions.isEmpty()) {
            evt.channel.sendMessage(
                "No definitions were found for `$wordOrPhrase`. Make sure it's spelled correctly."
            ).complete()
            return
        }
        if (index - 1 !in definitions.indices) {
            evt.channel.sendMessage(
                "There is no definition for `$wordOrPhrase` with the given index."
            ).complete()
            return
        }

        evt.channel.sendEmbed {
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

    @Command(description = "Gets synonyms and antonyms for the given word or phrase.", splitLastParameter = false)
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

    private data class WordnikConfig(var apiKey: String? = null)

    private val TokenStatus.isInvalid get() = !isValid
}