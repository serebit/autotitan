package listeners

import annotation.Command
import data.CommandData
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.io.File

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
                val commandName = when (annotation.name) {
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
          val command = commands[commandKey]
          if (command != null) {
            val method = command.method
            val argsList = getArgs(message, command)
            method.invoke(command.instance, evt, *argsList.toTypedArray())
          }
        }
      }
    }
  }

  fun getArgs(message: Message, command: CommandData): MutableList<Any> {
    val method = command.method
    val annotation = method.getAnnotation(Command::class.java)
    val splitArgs = message.content.split(" ").toMutableList<Any>()
    splitArgs.removeAt(0)
    if (annotation.delimitFinalParameter) {
      return splitArgs
    } else {
      val args = mutableListOf<Any>()
      for (i in (0..method.parameterCount - 2)) {
        args.add(splitArgs[i] as String)
        splitArgs.removeAt(0)
      }
      args.add(splitArgs.joinToString(" "))
      return args
    }
  }
}
