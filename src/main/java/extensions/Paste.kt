package extensions

import annotations.Command
import khttp.post
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Paste {
  val host = "https://hastebin.com"
  val path = "/documents"

  @Command(
      description = "Creates a paste in Hastebin with the given content and returns the URL.",
      delimitFinalParameter = false
  )
  fun paste(evt: MessageReceivedEvent, code: String) {
    val response = post(
        url = host + path,
        data = code
    )
    val url = host + "/" + response.jsonObject["key"]
    val message = "<@" + evt.author.id + ">'s paste: " + url
    evt.channel.deleteMessageById(evt.message.id).queue()
    evt.channel.sendMessage(message).queue()
  }
}
