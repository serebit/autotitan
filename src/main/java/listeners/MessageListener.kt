package listeners

import annotation.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.io.File
import java.lang.reflect.Method

class MessageListener(val commandPrefix: String) : ListenerAdapter() {
  val commands = mutableMapOf<String, CommandData>()

  init {
    val extensionsDirectory = File(ClassLoader.getSystemResource("extensions/").toURI())
    extensionsDirectory.list()
        .filter { it.endsWith(".class") }
        .forEach {
          val className = "extensions." + it.substring(0, it.length - 6)
          val extension = Class.forName(className)
          val instance = extension.newInstance()
          extension.methods
              .filter { it.isAnnotationPresent(Command::class.java) }
              .forEach {
                val annotation = it.getAnnotation(Command::class.java)
                val commandName = when(annotation.name) {
                  "" -> it.name
                  else -> annotation.name
                }
                commands.put(commandName, CommandData(instance, it))
              }
        }
  }

  override fun onMessageReceived(evt: MessageReceivedEvent) {
    val author = evt.author
    val message = evt.message
    if (!author.isBot) {
      if (message.content.startsWith(commandPrefix)) {
        val commandKey = message.content.split(" ")[0].substring(commandPrefix.length)
        if (commandKey in commands) {
          val commandData = commands[commandKey]
          command?.method.invoke(command.instance, evt)
        }
      }
    }
  }
}
