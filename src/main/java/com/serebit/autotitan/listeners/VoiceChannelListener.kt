package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.update.*
import net.dv8tion.jda.core.hooks.ListenerAdapter

class VoiceChannelListener(
    listeners: MutableSet<Listener>
) : ListenerAdapter(), EventListener {
  override val listeners = listeners.filter { it.eventType in validEventTypes }.toMutableSet()
  override val validEventTypes = mutableSetOf<Class<out Event>>(
      VoiceChannelDeleteEvent::class.java,
      VoiceChannelUpdateNameEvent::class.java,
      VoiceChannelUpdatePositionEvent::class.java,
      VoiceChannelUpdateUserLimitEvent::class.java,
      VoiceChannelUpdateBitrateEvent::class.java,
      VoiceChannelUpdatePermissionsEvent::class.java,
      VoiceChannelCreateEvent::class.java
  )

  override fun runListeners(evt: Event) {
    launch(CommonPool) {
      listeners
          .filter { it.eventType == evt::class.java }
          .forEach { it.method(it.instance, evt) }
    }
  }

  override fun onVoiceChannelDelete(evt: VoiceChannelDeleteEvent) {
    runListeners(evt)
  }

  override fun onVoiceChannelUpdateName(evt: VoiceChannelUpdateNameEvent) {
    runListeners(evt)
  }

  override fun onVoiceChannelUpdatePosition(evt: VoiceChannelUpdatePositionEvent) {
    runListeners(evt)
  }

  override fun onVoiceChannelUpdateUserLimit(evt: VoiceChannelUpdateUserLimitEvent) {
    runListeners(evt)
  }

  override fun onVoiceChannelUpdateBitrate(evt: VoiceChannelUpdateBitrateEvent) {
    runListeners(evt)
  }

  override fun onVoiceChannelUpdatePermissions(evt: VoiceChannelUpdatePermissionsEvent) {
    runListeners(evt)
  }

  override fun onVoiceChannelCreate(evt: VoiceChannelCreateEvent) {
    runListeners(evt)
  }
}