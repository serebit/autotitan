package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.user.*
import net.dv8tion.jda.core.hooks.ListenerAdapter

class UserListener(
    allListeners: MutableSet<Listener>
) : ListenerAdapter(), EventListener {
  override val listeners: MutableSet<Listener>
  override val validEventTypes = mutableSetOf<Class<out Event>>(
      UserNameUpdateEvent::class.java,
      UserAvatarUpdateEvent::class.java,
      UserOnlineStatusUpdateEvent::class.java,
      UserGameUpdateEvent::class.java,
      UserTypingEvent::class.java
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
}