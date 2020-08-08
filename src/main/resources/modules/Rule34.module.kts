import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.serebit.autotitan.api.LongString
import com.serebit.autotitan.api.command
import com.serebit.autotitan.api.module
import com.serebit.autotitan.extensions.sendEmbed
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

val gson = Gson()
val client = HttpClient()

suspend fun randomPostOrNull(provider: ImageProvider, tags: String): ApiPost? {
    val response: HttpResponse =
        client.get("https://${provider.baseUri}/index.php?page=dapi&s=post&q=index&tags=$tags&json=1")

    return if (response.status == HttpStatusCode.OK && response.readText().isNotBlank()) {
        gson.fromJson<List<ApiPost>>(response.readText())
            .filter { !it.image.endsWith(".webm") }
            .random()
    } else null
}

fun formatTags(tagString: String, delimiter: String = "+") = tagString
    .filter { it.isLetterOrDigit() || it.isWhitespace() || it in arrayOf('-', '_', '(', ')') }
    .replace("\\s+".toRegex(), delimiter)

enum class ImageProvider(val baseUri: String, val imageBaseUri: String) {
    RULE34XXX("rule34.xxx", "rule34.xxx/images"),
    GELBOORU("gelbooru.com", "simg3.gelbooru.com/images")
}

data class ApiPost(
    private val directory: String,
    val image: String
) {
    val rule34xxxImageUri get() = "https://${ImageProvider.RULE34XXX.imageBaseUri}/$directory/$image"
    val gelbooruImageUri get() = "https://${ImageProvider.GELBOORU.imageBaseUri}/$directory/$image"
}

module("Rule34", isOptional = true) {
    command("rule34", "Searches Rule34.xxx for the given tags and returns a random image.") { tagString: LongString ->
        if (isFromGuild && textChannel.isNSFW || !isFromGuild) {
            randomPostOrNull(ImageProvider.RULE34XXX, formatTags(tagString.value))?.let { post ->
                channel.sendEmbed {
                    setImage(post.rule34xxxImageUri)
                    setFooter("via Rule34.xxx", "https://rule34.xxx/favicon.png")
                }.queue()
            } ?: channel.sendMessage(
                "No images found on Rule34.xxx for `${formatTags(tagString.value, " ")}`."
            ).queue()
        } else channel.sendMessage("This command can only be used in channels marked as NSFW.").queue()
    }

    command(
        "gelbooru",
        "Searches Gelbooru.com for the given tags and returns a random image."
    ) { tagString: LongString ->
        if (isFromGuild && textChannel.isNSFW || !isFromGuild) {
            val formattedTags = formatTags(tagString.value)
            randomPostOrNull(ImageProvider.GELBOORU, formattedTags)?.let { post ->
                channel.sendEmbed {
                    setImage(post.gelbooruImageUri)
                    setFooter("via Gelbooru", "https://gelbooru.com/favicon.png")
                }.queue()
            } ?: channel.sendMessage("No images found on Gelbooru for `${formatTags(tagString.value, " ")}`.").queue()
        } else channel.sendMessage("This command can only be used in channels marked as NSFW.").queue()
    }
}
