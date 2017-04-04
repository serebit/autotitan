package extensions

import annotation.Command
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class General {
  @Command()
  fun test(evt: MessageReceivedEvent) {
    evt.channel.sendMessage("Hello World!").queue()
  }
  
  @Command()
  fun memberInfo(evt: MessageReceivedEvent, member: Member) {
    val embedBuilder = EmbedBuilder()
  }
}
