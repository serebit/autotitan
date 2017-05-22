package com.serebit.autotitan.extensions

import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ListenerFunction
import khttp.post
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Paste {
  @CommandFunction(
      description = "Creates a paste in Hastebin with the given content and returns the URL.",
      delimitFinalParameter = false
  )
  fun paste(evt: MessageReceivedEvent, code: String) {
    val message = "${evt.author.asMention}'s paste: ${getPasteUrl(code)}"
    if (evt.guild != null)
      evt.message.delete().queue()
    evt.channel.sendMessage(message).queue()
  }

  @ListenerFunction(
      description = "Automatically creates a paste in Hastebin if a user sends a" +
          " message with a code block longer than a certain length."
  )
  fun autopaste(evt: MessageReceivedEvent) {
    val messageContent = evt.message.rawContent
    val codeBlockRegex = "`{3}.*\n((?:.*\n)*?)`{3}".toRegex()
    if (codeBlockRegex.containsMatchIn(messageContent)) {
      val matches = codeBlockRegex.findAll(messageContent)
      val groups = matches
          .map { it.groups[1] }
          .filterNotNull()
      val codeBlocks = groups
          .map { it.value }
          .filter { it.exceedsLengthLimit() }
          .filterNotNull()
          .toMutableList()
      if (codeBlocks.isNotEmpty()) {
        codeBlocks.forEach {
          val message = "${evt.author.asMention}'s paste: ${getPasteUrl(it)}"
          if (evt.guild != null) {
            evt.message.delete().queue()
          }
          evt.channel.sendMessage(message).queue()
        }
      }
    }
  }

  fun getPasteUrl(code: String): String {
    val response = post(
        url = host + path,
        data = code
    )
    val url = host + "/" + response.jsonObject["key"]
    return url
  }

  fun String.exceedsLengthLimit(): Boolean {
    val lineCount = this.lines().size
    val characterCount = this.count()
    return (lineCount > lineLimit || characterCount > characterLimit)
  }

  companion object {
    const val host = "https://hastebin.com"
    const val path = "/documents"
    const val lineLimit = 10
    const val characterLimit = 512
  }
}
