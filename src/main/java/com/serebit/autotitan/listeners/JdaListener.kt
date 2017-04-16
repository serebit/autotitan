package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Listener
import net.dv8tion.jda.core.events.*
import net.dv8tion.jda.core.hooks.ListenerAdapter

class JdaListener(
    val listeners: MutableList<Listener>
) : ListenerAdapter() {
  override fun onReady(evt: ReadyEvent) {
    runListeners(evt)
  }

  override fun onResume(evt: ResumedEvent) {
    runListeners(evt)
  }

  override fun onReconnect(evt: ReconnectedEvent) {
    runListeners(evt)
  }

  override fun onDisconnect(evt: DisconnectEvent) {
    runListeners(evt)
  }

  override fun onShutdown(evt: ShutdownEvent) {
    runListeners(evt)
  }

  override fun onStatusChange(evt: StatusChangeEvent) {
    runListeners(evt)
  }

  override fun onException(evt: ExceptionEvent) {
    runListeners(evt)
  }

  fun runListeners(evt: Event) {
    listeners
        .filter { it.eventType in validEventTypes }
        .filter { it.eventType == evt::class.java }
        .forEach { it.method(it.instance, evt) }
  }

  companion object {
    val validEventTypes = mutableSetOf(
        ReadyEvent::class.java,
        ResumedEvent::class.java,
        ReconnectedEvent::class.java,
        DisconnectEvent::class.java,
        ShutdownEvent::class.java,
        StatusChangeEvent::class.java,
        ExceptionEvent::class.java
    )
  }
}