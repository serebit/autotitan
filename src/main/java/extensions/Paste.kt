package extensions

import annotations.CommandFunction
import annotations.ListenerFunction
import khttp.post
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Paste {
  val host = "https://hastebin.com"
  val path = "/documents"

  @CommandFunction(
      description = "Creates a paste in Hastebin with the given content and returns the URL.",
      delimitFinalParameter = false
  )
  fun paste(evt: MessageReceivedEvent, code: String) {
    val response = post(
        url = host + path,
        data = code
    )
    val url = host + "/" + response.jsonObject["key"]
    val message = evt.member.asMention + "'s paste: " + url
    evt.channel.deleteMessageById(evt.message.id).queue()
    evt.channel.sendMessage(message).queue()
  }
  
  @ListenerFunction(
      description = "Automatically creates a paste in Hastebin if a user sends a message with a code block longer than a certain length.",
      eventType = MessageReceivedEvent::class,
      serverOnly = true
  )
  fun autopaste(evt: MessageReceivedEvent) {
    // Does nothing at the moment
  }
}
