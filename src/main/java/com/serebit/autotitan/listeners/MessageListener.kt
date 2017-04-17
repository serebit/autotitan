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
    val commands: MutableSet<Command>,
    allListeners: MutableSet<Listener>
) : ListenerAdapter(), EventListener {
  override val listeners: MutableSet<Listener>
  override val validEventTypes = mutableSetOf(
      MessageReceivedEvent::class.java,
      MessageDeleteEvent::class.java,
      MessageBulkDeleteEvent::class.java,
      MessageEmbedEvent::class.java,
      MessageUpdateEvent::class.java,
      MessageReactionAddEvent::class.java,
      MessageReactionRemoveEvent::class.java,
      MessageReactionRemoveAllEvent::class.java
  )

  init {
    listeners = allListeners.filter { it.eventType in validEventTypes }.toMutableSet()
  }

  override fun runListeners(evt: Event) {
    launch(CommonPool) {
      listeners
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
    val strings = getMessageParameters(evt.message.rawContent, command)
    if (types.size != strings.size) return false
    return types.zip(strings).all {
      (type, string) ->
      validateParameter(evt, type, string)
    }
  }

  fun validateParameter(evt: MessageReceivedEvent, type: Class<*>, string: String): Boolean {
    return when (type) {
      Int::class.java -> string.toIntOrNull() != null
      Long::class.java -> string.toLongOrNull() != null
      Double::class.java -> string.toDoubleOrNull() != null
      Float::class.java -> string.toFloatOrNull() != null
      User::class.java -> {
        evt.jda.getUserById(string.removePrefix("<@").removeSuffix(">")) != null
      }
      Member::class.java -> {
        evt.guild.getMemberById(string.removePrefix("<@").removeSuffix(">")) != null
      }
      Channel::class.java -> evt.guild.getTextChannelById(string) != null
      String::class.java -> true
      else -> {
        println(type.canonicalName + " is not a valid parameter type.")
        false
      }
    }
  }

  fun castParameter(evt: MessageReceivedEvent, type: Class<*>, string: String): Any {
    return when (type) {
      Int::class.java -> string.toInt()
      Long::class.java -> string.toLong()
      Double::class.java -> string.toDouble()
      Float::class.java -> string.toFloat()
      User::class.java -> {
        evt.jda.getUserById(string.removePrefix("<@").removeSuffix(">"))
      }
      Member::class.java -> {
        evt.guild.getMemberById(string.removePrefix("<@").removeSuffix(">"))
      }
      Channel::class.java -> evt.guild.getTextChannelById(string)
      else -> string
    }
  }

  fun getCastParameters(evt: MessageReceivedEvent, command: Command): MutableList<Any> {
    val strings = getMessageParameters(evt.message.rawContent, command)
    return command.parameterTypes.zip(strings).map {
      (type, string) ->
      castParameter(evt, type, string)
    }.toMutableList()
  }

  fun getMessageParameters(messageContent: String, command: Command): MutableList<String> {
    val trimmedMessageContent = messageContent.removePrefix(commandPrefix + command.name).trim()
    val delimitFinalParameter = command.delimitFinalParameter
    val parameterCount = command.parameterTypes.size
    val splitParameters = trimmedMessageContent.split(" ").filter(String::isNotBlank).toMutableList()
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
