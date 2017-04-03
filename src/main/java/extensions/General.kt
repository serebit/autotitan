package extensions

import annotation.Command
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class General {
  @Command()
  fun test(evt: MessageReceivedEvent) {
    evt.channel.sendMessage("Hello World!").complete()
  }
}
