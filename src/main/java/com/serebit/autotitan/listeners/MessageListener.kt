package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.*
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class MessageListener(
    val commands: MutableSet<Command>,
    allListeners: MutableSet<Listener>
) : ListenerAdapter(), EventListener {
  override val listeners: MutableSet<Listener>
  override val validEventTypes = setOf(
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
  
  fun runCommands(evt: MessageReceivedEvent) {
    launch(CommonPool) {
      var messageContent = evt.message.rawContent
      if (messageContent == "${Command.prefix}help") sendCommandList(evt)
      val command = commands
          .filter { it.matches(evt) }
          .sortedBy { it.name.length }
          .lastOrNull()
      if (command != null) {
        command(evt)
      } else {
        commands
            .filter { it.roughlyMatches(evt) }
            .sortedBy { it.name.length }
            .lastOrNull()?.sendHelpMessage(evt)
      }
    }
  }

  override fun onMessageReceived(evt: MessageReceivedEvent) {
    if (!evt.author.isBot) {
      runCommands(evt)
      runListeners(evt)
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

  fun sendCommandList(evt: MessageReceivedEvent) {
    var list = "```markdown\n# Command List\n"
    val commandMap = commands
        .sortedBy { it.method.declaringClass.simpleName }
        .groupBy({ it.method.declaringClass })
    commandMap.forEach {
      list += "\n${it.key.simpleName}\n  "
      list += "${it.value.map { it.name }.joinToString("\n  ")}"
    }
    list += "\n```"
    evt.channel.sendMessage(list).queue()
  }
}
