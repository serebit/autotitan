package com.serebit.autotitan.extensions

import com.serebit.autotitan.annotations.CommandFunction
import com.serebit.autotitan.annotations.ListenerFunction
import khttp.post
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class Paste {
  val host = "https://hastebin.com"
  val path = "/documents"

  @CommandFunction(
      description = "Creates a paste in Hastebin with the given content and returns the URL.",
      delimitFinalParameter = false
  )
  fun paste(evt: MessageReceivedEvent, code: String) {
    val message = "${evt.author.asMention}'s paste: ${getPasteUrl(code)}"
    evt.message.delete().queue()
    evt.channel.sendMessage(message).queue()
  }

  @ListenerFunction(
      description = "Automatically creates a paste in Hastebin if a user sends a" +
          " message with a code block longer than a certain length."
  )
  fun autopaste(evt: GuildMessageReceivedEvent) {
    val messageContent = evt.message.rawContent
    val codeBlockRegex = "`{3}.*\n((?:.*\n)*?)`{3}".toRegex()
    if (codeBlockRegex.matches(messageContent)) {
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
          evt.message.delete().queue()
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
    val lineCount = this.count { it == '\n' }
    val characterCount = this.count()
    return (lineCount > 10 || characterCount > 512)
  }
}