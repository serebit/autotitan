package listeners

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class MessageListener(val commandPrefix: String) : ListenerAdapter() {
  override fun onMessageReceived(evt: MessageReceivedEvent) {
    if (!evt.author.isBot) {
      if (evt.message.content.startsWith(commandPrefix)) {
        val channel = evt.channel
        channel.sendMessage("Responding to prefix.").complete()
      }
    }
  }
}