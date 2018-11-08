import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.api.extensions.jda.sendEmbed
import com.serebit.autotitan.api.extensions.limitLengthTo
import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.LongString
import khttp.get
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.net.HttpURLConnection
import java.net.URLEncoder

object UrbanDictionaryApi {
    private val serializer = Gson()
    private val resultCache: MutableMap<String, List<Definition>> = mutableMapOf()

    fun hasDefinitions(query: String) = existsOrCacheDefinition(query) && resultCache[query]!!.isNotEmpty()

    fun numDefinitions(query: String) = if (existsOrCacheDefinition(query)) resultCache[query]!!.size else 0

    fun getDefinition(query: String, index: Int): Definition? = if (existsOrCacheDefinition(query)) {
        resultCache[query]?.getOrNull(index)
    } else null

    private fun existsOrCacheDefinition(query: String): Boolean = if (query in resultCache) true else {
        val response = get("https://api.urbandictionary.com/v0/define?term=${URLEncoder.encode(query, "UTF-8")}")
        when {
            response.statusCode == HttpURLConnection.HTTP_OK -> {
                resultCache[query] = serializer.fromJson<Result>(response.text).list
                true
            }
            response.jsonObject["result_type"] != "no_results" -> {
                resultCache[query] = emptyList()
                false
            }
            else -> false
        }
    }

    private data class Result(val list: List<Definition>)

    data class Definition(val definition: String, val permalink: String, val example: String)
}

fun sendUrbanDefinition(evt: MessageReceivedEvent, query: String, index: Int = 1) {
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

module("Dictionary", isOptional = true) {
    command(
        "urban",
        "Gets the Nth Urban Dictionary definition of the given query."
    ) { evt, index: Int, query: LongString -> sendUrbanDefinition(evt, query.value, index) }

    command("urban", "Gets the first Urban Dictionary definition of the given query.") { evt, query: LongString ->
        sendUrbanDefinition(evt, query.value)
    }
}
