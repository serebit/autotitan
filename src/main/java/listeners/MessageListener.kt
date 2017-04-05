package listeners

import annotation.Command
import data.CommandData
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
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
        if (commandKey in commands && commands[commandKey] != null) {
          val method = commands[commandKey].method
          if (matchesCommand(evt, method) {
            val parameters = getParameters(message, method)
            method.invoke(command.instance, evt, *parameters.toTypedArray())
          }
        }
      }
    }
  }
  
  fun matchesCommand(evt: MessageReceivedEvent, method: Method): Boolean {
    val methodParameters = method.parameters.toMutableList<Parameter>()
    val parameterStrings = getParameterStrings(evt, method)
    if (methodParameters.size != parameterStrings.size) return false
    var matchesCommand = true
    for (i in (0..methodParameters.size - 1)) {
      val methodParameter = methodParameters[i]
      val parameterString = parameterStrings[i]
      matchesCommand = when(methodParameter.getType()) {
        Int::class.java -> (string.toIntOrNull() != null)
        Long::class.java -> (string.toLongOrNull() != null)
        Double::class.java -> (string.toDoubleOrNull() != null)
        Float::class.java -> (string.toFloatOrNull() != null)
        User::class.java -> (evt.jda.getUserById(string) != null)
        Member::class.java -> (evt.guild.getMemberById(string) != null)
        String::class.java -> true
        else -> false
      }
      if (!matchesCommand) break
    }
    return matchesCommand
  }

  fun getParameters(evt: MessageReceivedEvent, method: Method): MutableList<Any> {
    val parameterStrings = getParameterStrings()
    return parameterStrings
  }
  
  fun getParameterStrings(evt: MessageReceivedEvent, method: Method): MutableList<String> {
    val delimitFinalParameter = method.getAnnotation(Command::class.java).delimitFinalParameter
    val parameterCount = method.parameterCount
    val splitParameters = evt.message.content.split(" ").toMutableList<String>()
    splitParameters.removeAt(0) // Remove the command call to isolate the parameters
    if (delimitFinalParameter) {
      return splitParameters
    } else {
      val parameters = mutableListOf<String>()
      for (i in (0..parameterCount - 3)) {
        parameters.add(splitParameters[i])
        splitParameters.removeAt(0)
      }
      parameters.add(splitParameters.joinToString(" "))
      return parameters
    }
  }
}
