package listeners

import annotation.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.io.File
import java.lang.reflect.Method

class MessageListener(val commandPrefix: String) : ListenerAdapter() {
  val commands: MutableMap<String, Method>

  init {
    val extensionsDirectory = File(ClassLoader.getSystemResource("extensions/").toURI())
    commands = mutableMapOf()
    extensionsDirectory.list()
        .filter { it.endsWith(".class") }
        .forEach {
          val className = "extensions." + it.substring(0, it.length - 6)
          val extension = Class.forName(className)
          extension.methods
              .filter { it.isAnnotationPresent(Command::class.java) }
              .forEach {
                val annotation = it.getAnnotation(Command::class.java)
                val commandName = when(annotation.name) {
                  "" -> it.name
                  else -> annotation.name
                }
                commands.put(commandName, it)
              }
        }
  }

  override fun onMessageReceived(evt: MessageReceivedEvent) {
    val author = evt.author
    val message = evt.message
    if (!author.isBot) {
      if (message.content.startsWith(commandPrefix)) {
        val commandKey = content.split(" ")[0].substring(commandPrefix.length)
        if (commandKey in commands) {
          val command = commands[key]
          command?.invoke(command.declaringClass.newInstance(), evt)
        }
      }
    }
  }
}
