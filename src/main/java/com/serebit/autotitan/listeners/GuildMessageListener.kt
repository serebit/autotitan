package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.GuildCommand
import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageEmbedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class GuildMessageListener(
    val commands: MutableSet<GuildCommand>,
    unfilteredListeners: MutableSet<Listener>
) : ListenerAdapter(), EventListener {
  override val listeners: MutableSet<Listener>
  override val validEventTypes = mutableSetOf<Class<out Event>>(
      GuildMessageReceivedEvent::class.java,
      GuildMessageUpdateEvent::class.java,
      GuildMessageEmbedEvent::class.java,
      GuildMessageDeleteEvent::class.java
  )

  init {
    listeners = unfilteredListeners.filter { validEventTypes.contains(it.eventType) }.toMutableSet()
  }

  override fun runListeners(evt: Event) {
    launch(CommonPool) {
      listeners
          .filter { it.eventType == evt::class.java }
          .forEach { it.method(it.instance, evt) }
    }
  }

  override fun onGuildMessageReceived(evt: GuildMessageReceivedEvent) {
    if (!evt.author.isBot) {
      runListeners(evt)
    }
  }

  override fun onGuildMessageDelete(evt: GuildMessageDeleteEvent) {
    runListeners(evt)
  }

  override fun onGuildMessageEmbed(evt: GuildMessageEmbedEvent) {
    runListeners(evt)
  }

  override fun onGuildMessageUpdate(evt: GuildMessageUpdateEvent) {
    runListeners(evt)
  }
}