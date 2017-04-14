package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Command
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class MessageListener(
    val commandPrefix: String,
    val commands: MutableList<Command>
) : ListenerAdapter() {
  override fun onMessageReceived(evt: MessageReceivedEvent) {
    var messageContent = evt.message.rawContent
    if (!evt.author.isBot && messageContent.startsWith(commandPrefix)) {
      messageContent = messageContent.removePrefix(">")
      val command = commands.filter {
        messageContent.startsWith(it.name + " ") || messageContent == it.name
      }.sortedBy { it.name.length }.lastOrNull()
      if (command != null) {
        if (matchesCommand(evt, command)) {
          val parameters = getCastParameters(evt, command)
          command.method.invoke(command.instance, evt, *parameters.toTypedArray())
        } else {
          val helpMessage = "```\n$commandPrefix${command.name} " +
              command.parameterTypes
                  .map(Class<*>::getSimpleName)
                  .joinToString(" ") +
              "\n\n${command.description}```"
          evt.channel.sendMessage(helpMessage).queue()
        }
      }
    }
  }

  fun matchesCommand(evt: MessageReceivedEvent, command: Command): Boolean {
    val types = command.parameterTypes
    if (evt.message.rawContent.startsWith(commandPrefix + command.name)) {
      val messageContent = evt.message.rawContent.removePrefix(commandPrefix + command.name).trim()
      val strings = getMessageParameters(messageContent, command)
      if (types.size != strings.size) return false
      return types.zip(strings).all {
        (type, string) ->
        validateParameter(evt, type, string)
      }
    } else {
      return false
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

  fun getCastParameters(evt: MessageReceivedEvent, command: Command): MutableList<Any> {
    val messageContent = evt.message.rawContent.removePrefix(commandPrefix + command.name)
    val strings = getMessageParameters(messageContent, command)
    return command.parameterTypes.zip(strings).map {
      (type, string) ->
      castParameter(evt, type, string)
    }.toMutableList()
  }

  fun getMessageParameters(messageContent: String, command: Command): MutableList<String> {
    val delimitFinalParameter = command.delimitFinalParameter
    val parameterCount = command.parameterTypes.size
    val splitParameters = messageContent.split(" ").toMutableList()
    splitParameters.removeAt(0) // Remove the command call
    if (delimitFinalParameter) {
      return splitParameters
    } else {
      val parameters = mutableListOf<String>()
      (0..parameterCount - 2).forEach {
        parameters.add(splitParameters[it])
        splitParameters.removeAt(0)
      }
      if (splitParameters.size > 0) {
        parameters.add(splitParameters.joinToString(" "))
      }
      return parameters
    }
  }
}
