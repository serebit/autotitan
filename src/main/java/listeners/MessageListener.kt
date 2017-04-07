package listeners

import annotations.Command
import data.BotCommand
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.io.File
import java.lang.reflect.Method

class MessageListener(val commandPrefix: String) : ListenerAdapter() {
  val commands = mutableMapOf<String, BotCommand>()

  init {
    val extensionsDirectory = File(ClassLoader.getSystemResource("extensions/").toURI())
    extensionsDirectory.list()
        .filter { it.endsWith(".class") }
        .forEach {
          val extension = Class.forName("extensions." + it.substring(0, it.length - 6))
          extension.methods
              .filter { it.isAnnotationPresent(Command::class.java) }
              .filter { it.parameterTypes[0] == MessageReceivedEvent::class.java }
              .forEach {
                val annotation = it.getAnnotation(Command::class.java)
                val commandName = when (annotation.name) {
                  "" -> it.name
                  else -> annotation.name
                }
                commands.put(commandName.toLowerCase(), BotCommand(extension.newInstance(), it))
              }
        }
  }

  override fun onMessageReceived(evt: MessageReceivedEvent) {
    val author = evt.author
    val message = evt.message
    if (!author.isBot) {
      if (message.content.startsWith(commandPrefix)) {
        val commandKey = message.content.split(" ")[0].substring(commandPrefix.length).toLowerCase()
        if (commandKey in commands) {
          val command = commands[commandKey]
          if (command != null) {
            if (matchesCommand(evt, command.method)) {
              val parameters = getParameters(evt, command.method)
              command.method.invoke(command.instance, evt, *parameters.toTypedArray())
            }
          }
        }
      }
    }
  }

  fun matchesCommand(evt: MessageReceivedEvent, method: Method): Boolean {
    val types = method.parameterTypes.toMutableList()
    types.removeAt(0) // Remove MessageReceivedEvent parameter
    val strings = getParameterStrings(evt, method)
    if (types.size != strings.size) return false
    return types.zip(strings).all {
      (type, string) ->
      validateParameter(evt, type, string)
    }
  }

  fun validateParameter(evt: MessageReceivedEvent, type: Class<*>, string: String): Boolean {
    return when (type) {
      Int::class.java -> string.toIntOrNull() as Any
      Long::class.java -> string.toLongOrNull() as Any
      Double::class.java -> string.toDoubleOrNull() as Any
      Float::class.java -> string.toFloatOrNull() as Any
      User::class.java -> evt.jda.getUserById(string)
      Member::class.java -> evt.guild.getMemberById(string)
      Channel::class.java -> evt.guild.getTextChannelById(string)
      String::class.java -> "This can be anything but null. ¯\\_(ツ)_/¯"
      else -> {
        println(type.canonicalName + " is not a valid parameter type.")
        null
      }
    } != null
  }

  fun getParameters(evt: MessageReceivedEvent, method: Method): MutableList<Any> {
    val parameterStrings = getParameterStrings(evt, method)
    return parameterStrings.toMutableList<Any>()
  }

  fun getParameterStrings(evt: MessageReceivedEvent, method: Method): MutableList<String> {
    val delimitFinalParameter = method.getAnnotation(Command::class.java).delimitFinalParameter
    val parameterCount = method.parameterCount
    val splitParameters = evt.message.rawContent.split(" ").toMutableList()
    splitParameters.removeAt(0) // Remove the command call to isolate the parameters
    if (delimitFinalParameter) {
      return splitParameters
    } else {
      val parameters = mutableListOf<String>()
      for (i in (0..parameterCount - 3)) {
        parameters.add(splitParameters[i])
        splitParameters.removeAt(0)
      }
      if (splitParameters.size > 0) {
        parameters.add(splitParameters.joinToString(" "))
      }
      return parameters
    }
  }
}
