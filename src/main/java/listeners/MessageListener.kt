package listeners

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class MessageListener : ListenerAdapter() {
  override fun onMessageReceived(evt: MessageReceivedEvent) {
    if (!evt.author.isBot) {
      if (evt.message.content == "/>test") {
        val channel = evt.channel
        channel.sendMessage("Responding to test.").complete()
      }
    }
  }
}