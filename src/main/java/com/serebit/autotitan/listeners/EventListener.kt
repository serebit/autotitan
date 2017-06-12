package com.serebit.autotitan.listeners

import com.serebit.autotitan.config
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class EventListener(
    val commands: Set<Command>,
    val listeners: Set<Listener>
) : ListenerAdapter() {
  override fun onGenericEvent(evt: Event) {
    if (evt is MessageReceivedEvent && evt.message.rawContent.trim().startsWith(config.prefix)) {
      runCommands(evt)
    }
    runListeners(evt)
  }

  fun runListeners(evt: Event) {
    listeners.filter { it.eventType == evt::class.java }
        .forEach {
          launch(CommonPool) {
            it.method(it.instance, evt)
          }
        }
  }

  fun runCommands(evt: MessageReceivedEvent) {
    launch(CommonPool) {
      val messageContent = evt.message.rawContent.trim()
      if (messageContent == "${config.prefix}help") sendCommandList(evt)
      val command = commands
          .filter { it.matches(evt) }
          .sortedBy { it.name.length }
          .lastOrNull()
      if (command != null) {
        command(evt)
      } else {
        commands.filter { it.roughlyMatches(evt) }
            .sortedBy { it.name.length }
            .lastOrNull()?.sendHelpMessage(evt)
      }
    }
  }

  fun sendCommandList(evt: MessageReceivedEvent) {
    val embedBuilder = EmbedBuilder().apply {
      setColor(evt.guild?.selfMember?.color)
    }
    val commandMap = commands.sortedBy { it.method.declaringClass.simpleName }
        .groupBy({ it.method.declaringClass })
    commandMap.forEach {
      val title = it.key.simpleName
      val content = it.value.map {
        "`${it.name}`" + if(it.description.isNotEmpty()) " - ${it.description}" else ""
      }.joinToString("\n")
      embedBuilder.addField(title, content, false)
    }
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }
}