package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.*
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class MessageListener(
    val commandPrefix: String,
    val commands: MutableList<Command>,
    val listeners: MutableList<Listener>
) : ListenerAdapter(), EventListener {
  override fun runListeners(evt: Event) {
    launch(CommonPool) {
      listeners
          .filter { it.eventType in validEventTypes }
          .filter { it.eventType == evt::class.java }
          .forEach { it.method(it.instance, evt) }
    }
  }

  override fun onMessageReceived(evt: MessageReceivedEvent) {
    if (!evt.author.isBot) {
      runListeners(evt)
      var messageContent = evt.message.rawContent
      if (messageContent.startsWith(commandPrefix)) {
        messageContent = messageContent.removePrefix(">")
        if (messageContent == "help") {
          sendCommandList(evt)
        }
        val command = commands.filter {
          messageContent.startsWith(it.name + " ") || messageContent == it.name
        }.sortedBy { it.name.length }.lastOrNull()
        if (command != null) {
          if (matchesCommand(evt, command)) {
            val parameters = getCastParameters(evt, command)
            command.method(command.instance, evt, *parameters.toTypedArray())
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
  }

  override fun onMessageDelete(evt: MessageDeleteEvent) {
    runListeners(evt)
  }

  override fun onMessageBulkDelete(evt: MessageBulkDeleteEvent) {
    runListeners(evt)
  }

  override fun onMessageEmbed(evt: MessageEmbedEvent) {
    runListeners(evt)
  }

  override fun onMessageUpdate(evt: MessageUpdateEvent) {
    runListeners(evt)
  }

  override fun onMessageReactionAdd(evt: MessageReactionAddEvent) {
    runListeners(evt)
  }

  override fun onMessageReactionRemove(evt: MessageReactionRemoveEvent) {
    runListeners(evt)
  }

  override fun onMessageReactionRemoveAll(evt: MessageReactionRemoveAllEvent) {
    runListeners(evt)
  }

  override fun onGenericMessage(evt: GenericMessageEvent) {
    runListeners(evt)
  }

  fun sendCommandList(evt: MessageReceivedEvent) {
    val list = "```\nCommand List\n\n" +
        commands
            .sortedBy { it.method.declaringClass.simpleName }
            .map {
          "${it.name} " + it.parameterTypes
            .map { "<" + it.simpleName + ">" }
            .joinToString(" ")
        }.joinToString("\n") +
        "\n```"
    evt.channel.sendMessage(list).queue()
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

  companion object {
    val validEventTypes = mutableSetOf(
        MessageReceivedEvent::class.java,
        MessageDeleteEvent::class.java,
        MessageBulkDeleteEvent::class.java,
        MessageEmbedEvent::class.java,
        MessageUpdateEvent::class.java,
        MessageReactionAddEvent::class.java,
        MessageReactionRemoveEvent::class.java,
        MessageReactionRemoveAllEvent::class.java
    )
  }
}
