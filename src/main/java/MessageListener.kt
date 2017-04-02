import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class MessageListener : ListenerAdapter() {
  override fun onMessageReceived(evt: MessageReceivedEvent) {
    if (!evt.author.isBot) {
      val channel = evt.channel
      channel.sendMessage(evt.message.content).queue()
    }
  }
}