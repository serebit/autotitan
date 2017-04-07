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
    val message = evt.member.mention + "'s paste: " + url
    evt.channel.deleteMessageById(evt.message.id).queue()
    evt.channel.sendMessage(message).queue()
  }
  
  @Listener(
      description = "Automatically creates a paste in Hastebin if a user sends a message with a code block longer than a certain length.",
      serverOnly = true
  )
  fun autopaste(evt: MessageReceivedEvent) {
    // Does nothing at the moment
  }
}
