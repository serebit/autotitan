
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.*

fun main(args: Array<String>) {
  print("Enter token: ")
  val token = Scanner(System.`in`).nextLine()
  val jda = JDABuilder(AccountType.BOT)
      .setToken(token)
      .buildBlocking()
  jda.addEventListener(MessageListener())
}

class MessageListener : ListenerAdapter() {
  override fun onMessageReceived(evt: MessageReceivedEvent) {
    if (!evt.author.isBot) {
      val channel = evt.channel
      println(evt.message.content)
      channel.sendMessage(evt.message.content).queue()
    }
  }
}