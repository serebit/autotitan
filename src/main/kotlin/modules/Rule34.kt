package com.serebit.autotitan.modules

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.extensions.jda.sendEmbed
import com.serebit.autotitan.api.parameters.LongString
import khttp.get
import java.net.HttpURLConnection

@Suppress("UNUSED")
class Rule34 : ModuleTemplate(isOptional = true) {
    private val gson = Gson()

    init {
        command(
            "rule34",
            "Searches Rule34.xxx for the given tags and returns a random image."
        ) { evt, tagString: LongString ->
            if (evt.guild != null && evt.textChannel.isNSFW || evt.privateChannel != null) {
                randomPostOrNull(ImageProvider.RULE34XXX, formatTags(tagString.value))?.let { post ->
                    evt.channel.sendEmbed {
                        setImage(post.rule34xxxImageUri)
                        setFooter("via Rule34.xxx", "https://rule34.xxx/favicon.png")
                    }.queue()
                } ?: evt.channel.sendMessage(
                    "No images found on Rule34.xxx for `${formatTags(tagString.value, " ")}`."
                ).queue()
            } else evt.channel.sendMessage("This command can only be used in channels marked as NSFW.").queue()
        }

        command(
            "gelbooru",
            "Searches Gelbooru.com for the given tags and returns a random image."
        ) { evt, tagString: LongString ->
            if (evt.guild != null && evt.textChannel.isNSFW || evt.privateChannel != null) {
                val formattedTags = formatTags(tagString.value)
                randomPostOrNull(ImageProvider.GELBOORU, formattedTags)?.let { post ->
                    evt.channel.sendEmbed {
                        setImage(post.gelbooruImageUri)
                        setFooter("via Gelbooru", "https://gelbooru.com/favicon.png")
                    }.queue()
                } ?: evt.channel.sendMessage(
                    "No images found on Gelbooru for `${formatTags(tagString.value, " ")}`."
                ).queue()
            } else evt.channel.sendMessage("This command can only be used in channels marked as NSFW.").queue()
        }
    }

    private fun randomPostOrNull(provider: ImageProvider, tags: String): ApiPost? {
        val response = get("https://${provider.baseUri}/index.php?page=dapi&s=post&q=index&tags=$tags&json=1")
        return if (response.statusCode == HttpURLConnection.HTTP_OK && response.text.isNotBlank()) {
            gson.fromJson<List<ApiPost>>(response.text)
                .filter { !it.image.endsWith(".webm") }
                .random()
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
}
