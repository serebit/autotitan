package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Listener
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.user.*
import net.dv8tion.jda.core.hooks.ListenerAdapter

class UserListener(
    val listeners: MutableList<Listener>
) : ListenerAdapter(), EventListener {
  override fun runListeners(evt: Event) {
    listeners
        .filter { it.eventType in validEventTypes }
        .filter { it.eventType == evt::class.java }
        .forEach { it.method(it.instance, evt) }
  }

  override fun onUserNameUpdate(evt: UserNameUpdateEvent) {
    runListeners(evt)
  }

  override fun onUserAvatarUpdate(evt: UserAvatarUpdateEvent) {
    runListeners(evt)
  }

  override fun onUserOnlineStatusUpdate(evt: UserOnlineStatusUpdateEvent) {
    runListeners(evt)
  }

  override fun onUserGameUpdate(evt: UserGameUpdateEvent) {
    runListeners(evt)
  }

  override fun onUserTyping(evt: UserTypingEvent) {
    runListeners(evt)
  }

  companion object {
    val validEventTypes = mutableSetOf<Class<out Event>>(
        UserNameUpdateEvent::class.java,
        UserAvatarUpdateEvent::class.java,
        UserOnlineStatusUpdateEvent::class.java,
        UserGameUpdateEvent::class.java,
        UserTypingEvent::class.java
    )
  }
}