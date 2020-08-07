package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.ModuleCompanion
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.extensions.sendEmbed
import com.serebit.autotitan.extensions.truncate
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.net.URLEncoder

@Suppress("UNUSED", "TooManyFunctions")
class Dictionary : Module(isOptional = true) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = HttpClient()

    @Command(description = "Gets the first Urban Dictionary definition of the given query.", splitLastParameter = false)
    fun urban(evt: MessageReceivedEvent, query: String) = urban(evt, 1, query)

    @Command(description = "Gets the Nth Urban Dictionary definition of the given query.", splitLastParameter = false)
    fun urban(evt: MessageReceivedEvent, index: Int, query: String) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        scope.launch {
            val apiResult: HttpResponse = client.get("https://api.urbandictionary.com/v0/define?term=$encodedQuery")
            val definitions = Json.decodeFromString<UrbanDictionaryResult>(apiResult.readText()).list
            if (definitions.isNotEmpty()) {
                val definition = definitions[index - 1]
                    .definition
                    .replace("\\[word]".toRegex(), "")
                    .replace("\\[(.+?)]".toRegex(), "$1")
                evt.channel.sendEmbed {
                    setTitle("$query (Definition $index of ${definitions.size})", definitions[0].permalink)
                    setDescription(definition.truncate(MessageEmbed.VALUE_MAX_LENGTH))
                    setFooter(
                        "Powered by Urban Dictionary",
                        "https://res.cloudinary.com/hrscywv4p/image/upload/v1/1194347/vo5ge6mdw4creyrgaq2m.png"
                    )
                }.complete()
            } else evt.channel.sendMessage("Couldn't find a definition at that index.").complete()
        }
    }

    @Serializable
    private data class UrbanDictionaryResult(val list: List<UrbanDictionaryDefinition>)

    @Serializable
    private data class UrbanDictionaryDefinition(val definition: String, val permalink: String, val example: String)

    companion object : ModuleCompanion {
        override fun provide() = Dictionary()
    }
}
