package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.ModuleCompanion
import com.serebit.autotitan.api.annotations.Command
import com.serebit.autotitan.extensions.sendEmbed
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

@Suppress("UNUSED")
class Rule34 : Module(isOptional = true) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val client = HttpClient()

    @Command(
        description = "Searches Rule34.xxx for the given tags and returns a random image.",
        splitLastParameter = false
    )
    fun rule34(evt: MessageReceivedEvent, tagString: String) {
        if (evt.isFromGuild && evt.textChannel.isNSFW || !evt.isFromGuild) {
            scope.launch {
                randomPostOrNull(ImageProvider.RULE34XXX, formatTags(tagString))?.let { post ->
                    evt.channel.sendEmbed {
                        setImage(post.rule34xxxImageUri)
                        setFooter("via Rule34.xxx", "https://rule34.xxx/favicon.png")
                    }.complete()
                } ?: evt.channel.sendMessage(
                    "No images found on Rule34.xxx for `${formatTags(tagString, " ")}`."
                ).complete()
            }
        } else evt.channel.sendMessage("This command can only be used in channels marked as NSFW.").complete()
    }

    @Command(
        description = "Searches Gelbooru.com for the given tags and returns a random image.",
        splitLastParameter = false
    )
    fun gelbooru(evt: MessageReceivedEvent, tagString: String) {

        if (evt.isFromGuild && evt.textChannel.isNSFW || !evt.isFromGuild) {
            val formattedTags = formatTags(tagString)
            scope.launch {
                randomPostOrNull(ImageProvider.GELBOORU, formattedTags)?.let { post ->
                    evt.channel.sendEmbed {
                        setImage(post.gelbooruImageUri)
                        setFooter("via Gelbooru", "https://gelbooru.com/favicon.png")
                    }.complete()
                } ?: evt.channel.sendMessage(
                    "No images found on Gelbooru for `${formatTags(tagString, " ")}`."
                ).complete()
            }
        } else evt.channel.sendMessage("This command can only be used in channels marked as NSFW.").complete()
    }

    private suspend fun randomPostOrNull(provider: ImageProvider, tags: String): ApiPost? {
        val response: HttpResponse =
            client.get("https://${provider.baseUri}/index.php?page=dapi&s=post&q=index&tags=$tags&json=1")
        return if (response.status.isSuccess() && response.readText().isNotBlank()) {
            Json.decodeFromString<List<ApiPost>>(response.readText())
                .filter { !it.image.endsWith(".webm") }
                .randomOrNull()
        } else null
    }

    private fun formatTags(tagString: String, delimiter: String = "+") = tagString
        .filter { it.isLetterOrDigit() || it.isWhitespace() || it in arrayOf('-', '_', '(', ')') }
        .replace("\\s+".toRegex(), delimiter)

    private enum class ImageProvider(val baseUri: String, val imageBaseUri: String) {
        RULE34XXX("rule34.xxx", "rule34.xxx/images"),
        GELBOORU("gelbooru.com", "simg3.gelbooru.com/images")
    }

    private data class ApiPost(
        private val directory: String,
        val image: String
    ) {
        val rule34xxxImageUri get() = "https://${ImageProvider.RULE34XXX.imageBaseUri}/$directory/$image"
        val gelbooruImageUri get() = "https://${ImageProvider.GELBOORU.imageBaseUri}/$directory/$image"
    }

    companion object : ModuleCompanion {
        override fun provide() = Rule34()
    }
}
