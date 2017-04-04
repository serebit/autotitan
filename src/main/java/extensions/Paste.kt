package extensions

import annotation.Command
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Paste {
  @Command()
  fun paste(evt: MessageReceivedEvent, code: String) {
    evt.channel.sendMessage("Paste is unavailable at this time.").queue()
  }
}
