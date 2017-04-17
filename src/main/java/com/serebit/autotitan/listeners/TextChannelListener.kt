package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateNameEvent
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdatePermissionsEvent
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdatePositionEvent
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateTopicEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class TextChannelListener(
    listeners: MutableSet<Listener>
) : ListenerAdapter(), EventListener {
  override val listeners = listeners.filter { it.eventType in validEventTypes }.toMutableSet()
  override val validEventTypes = mutableSetOf<Class<out Event>>(
      TextChannelDeleteEvent::class.java,
      TextChannelUpdateNameEvent::class.java,
      TextChannelUpdateTopicEvent::class.java,
      TextChannelUpdatePositionEvent::class.java,
      TextChannelUpdatePermissionsEvent::class.java,
      TextChannelCreateEvent::class.java
  )

  override fun runListeners(evt: Event) {
    launch(CommonPool) {
      listeners
          .filter { it.eventType == evt::class.java }
          .forEach { it.method(it.instance, evt) }
    }
  }

  override fun onTextChannelDelete(evt: TextChannelDeleteEvent) {
    runListeners(evt)
  }

  override fun onTextChannelUpdateName(evt: TextChannelUpdateNameEvent) {
    runListeners(evt)
  }

  override fun onTextChannelUpdateTopic(evt: TextChannelUpdateTopicEvent) {
    runListeners(evt)
  }

  override fun onTextChannelUpdatePosition(evt: TextChannelUpdatePositionEvent) {
    runListeners(evt)
  }

  override fun onTextChannelUpdatePermissions(evt: TextChannelUpdatePermissionsEvent) {
    runListeners(evt)
  }

  override fun onTextChannelCreate(evt: TextChannelCreateEvent) {
    runListeners(evt)
  }
}