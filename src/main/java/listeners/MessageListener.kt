package listeners

import annotations.Command
import com.google.common.reflect.ClassPath
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
    loadExtensions()
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
            val instance = command.instance
            val method = command.method
            val annotation = method.getAnnotation(Command::class.java)
            if (matchesCommand(evt, method)) {
              val parameters = getParameters(evt, method)
              method.invoke(instance, evt, *parameters.toTypedArray())
            } else {
              val helpMessage = "```\n$commandPrefix$commandKey " +
                  getMethodParameterTypes(command.method)
                      .map(Class<*>::getSimpleName)
                      .joinToString(" ") +
                  "\n\n```"
              evt.channel.sendMessage(helpMessage).queue()
            }
          }
        }
      }
    }
  }

  fun matchesCommand(evt: MessageReceivedEvent, method: Method): Boolean {
    val types = getMethodParameterTypes(method)
    val strings = getMessageParameters(evt, method)
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

  fun castParameter(evt: MessageReceivedEvent, type: Class<*>, string: String): Any {
    return when (type) {
      Int::class.java -> string.toInt()
      Long::class.java -> string.toLong()
      Double::class.java -> string.toDouble()
      Float::class.java -> string.toFloat()
      User::class.java -> evt.jda.getUserById(string)
      Member::class.java -> evt.guild.getMemberById(string)
      Channel::class.java -> evt.guild.getTextChannelById(string)
      else -> string
    }
  }

  fun getParameters(evt: MessageReceivedEvent, method: Method): MutableList<Any> {
    val strings = getMessageParameters(evt, method)
    val types = getMethodParameterTypes(method)
    return types.zip(strings).map {
      (type, string) ->
      castParameter(evt, type, string)
    }.toMutableList()
  }

  fun getMessageParameters(evt: MessageReceivedEvent, method: Method): MutableList<String> {
    val delimitFinalParameter = method.getAnnotation(Command::class.java).delimitFinalParameter
    val parameterCount = getMethodParameterTypes(method).size
    val splitParameters = evt.message.rawContent.split(" ").toMutableList()
    splitParameters.removeAt(0) // Remove the command call
    if (delimitFinalParameter) {
      return splitParameters
    } else {
      val parameters = mutableListOf<String>()
      for (i in (0..parameterCount - 2)) {
        parameters.add(splitParameters[i])
        splitParameters.removeAt(0)
      }
      if (splitParameters.size > 0) {
        parameters.add(splitParameters.joinToString(" "))
      }
      return parameters
    }
  }

  fun getMethodParameterTypes(method: Method): MutableList<Class<*>> {
    val types = method.parameterTypes.toMutableList()
    types.removeAt(0) // Get rid of MessageReceivedEvent parameter
    return types
  }

  fun loadExtensions() {
    val cp = ClassPath.from(Thread.currentThread().contextClassLoader)
    cp.getTopLevelClassesRecursive("extensions")
        .forEach { extensionClass ->
          run {
            val extension = extensionClass.load()
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
  }
}
