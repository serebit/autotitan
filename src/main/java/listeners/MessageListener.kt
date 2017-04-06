package listeners

import annotation.Command
import data.CommandData
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
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
    val methodParameterTypes = method.parameterTypes.toMutableList()
    methodParameterTypes.removeAt(0) // Remove MessageReceivedEvent parameter
    val parameterStrings = getParameterStrings(evt, method)
    if (methodParameterTypes.size != parameterStrings.size) return false
    var matchesCommand = true
    for (i in (0..methodParameterTypes.size - 1)) {
      val methodParameterType = methodParameterTypes[i]
      val parameterString = parameterStrings[i]
      matchesCommand = when(methodParameterType) {
        Int::class.java -> (parameterString.toIntOrNull() != null)
        Long::class.java -> (parameterString.toLongOrNull() != null)
        Double::class.java -> (parameterString.toDoubleOrNull() != null)
        Float::class.java -> (parameterString.toFloatOrNull() != null)
        User::class.java -> (evt.jda.getUserById(parameterString) != null)
        Member::class.java -> (evt.guild.getMemberById(parameterString) != null)
        String::class.java -> true
        else -> {
          println(methodParameterType.canonicalName + " is not a valid parameter type.")
          false
        }
      }
      if (!matchesCommand) break
    }
    return matchesCommand
  }

  fun getParameters(evt: MessageReceivedEvent, method: Method): MutableList<Any> {
    val parameterStrings = getParameterStrings(evt, method)
    return parameterStrings.toMutableList<Any>()
  }
  
  fun getParameterStrings(evt: MessageReceivedEvent, method: Method): MutableList<String> {
    val delimitFinalParameter = method.getAnnotation(Command::class.java).delimitFinalParameter
    val parameterCount = method.parameterCount
    val splitParameters = evt.message.content.split(" ").toMutableList()
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
