package com.serebit.autotitan.extensions

import com.serebit.autotitan.annotations.CommandFunction
import com.serebit.autotitan.annotations.ListenerFunction
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
      description = "Automatically creates a paste in Hastebin if a user sends a" +
          " message with a code block longer than a certain length.",
      serverOnly = true
  )
  fun autopaste(evt: MessageReceivedEvent) {
    val messageContent = evt.message.rawContent
    val codeBlockRegex = "`{3}.*\n((?:.*\n)*?)`{3}".toRegex()
    if (codeBlockRegex.matches(messageContent)) {
      val matches = codeBlockRegex.findAll(messageContent)
      val groups: MutableList<MatchGroup> = matches
          .map { it.groups[0] }
          .filterNotNull()
          .toMutableList()
      val codeBlocks = groups
          .map { it.value }
          .filter { it.exceedsLengthLimit() }
          .filterNotNull()
      if (codeBlocks.isNotEmpty()) {
        codeBlocks.forEach { paste(evt, it) }
        evt.message.delete().queue()
      }
    }
  }

  fun String.exceedsLengthLimit(): Boolean {
    val lineCount = this.count { it == '\n' }
    val characterCount = this.count()
    return (lineCount > 10 || characterCount > 512)
  }
}
