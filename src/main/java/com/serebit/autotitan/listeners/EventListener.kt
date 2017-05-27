package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class EventListener(
    val commands: MutableSet<Command>,
    val listeners: MutableSet<Listener>
) : ListenerAdapter() {
  override fun onGenericEvent(evt: Event) {
    if (evt is MessageReceivedEvent) runCommands(evt)
    runListeners(evt)
  }

  fun runListeners(evt: Event) {
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