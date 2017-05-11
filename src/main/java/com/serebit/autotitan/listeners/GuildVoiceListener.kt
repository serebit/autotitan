package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.voice.*
import net.dv8tion.jda.core.hooks.ListenerAdapter

/**
 * Created by gingerdeadshot on 5/11/17.
 */
class GuildVoiceListener(
    unfilteredListeners: MutableSet<Listener>
) : ListenerAdapter(), EventListener {
  override val listeners: MutableSet<Listener>
  override val validEventTypes = setOf<Class<out Event>>(
      GuildVoiceLeaveEvent::class.java,
      GuildVoiceDeafenEvent::class.java,
      GuildVoiceGuildDeafenEvent::class.java,
      GuildVoiceGuildMuteEvent::class.java,
      GuildVoiceJoinEvent::class.java,
      GuildVoiceMoveEvent::class.java,
      GuildVoiceMuteEvent::class.java,
      GuildVoiceSelfDeafenEvent::class.java,
      GuildVoiceSelfMuteEvent::class.java,
      GuildVoiceSuppressEvent::class.java
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

  override fun onGuildVoiceMute(evt: GuildVoiceMuteEvent) {
    runListeners(evt)
  }

  override fun onGuildVoiceDeafen(evt: GuildVoiceDeafenEvent) {
    runListeners(evt)
  }

  override fun onGuildVoiceGuildDeafen(evt: GuildVoiceGuildDeafenEvent) {
    runListeners(evt)
  }

  override fun onGuildVoiceGuildMute(evt: GuildVoiceGuildMuteEvent) {
    runListeners(evt)
  }

  override fun onGuildVoiceJoin(evt: GuildVoiceJoinEvent) {
    runListeners(evt)
  }

  override fun onGuildVoiceLeave(evt: GuildVoiceLeaveEvent) {
    if (!evt.member.user.isBot) {
      runListeners(evt)
    }
  }

  override fun onGuildVoiceMove(evt: GuildVoiceMoveEvent) {
    runListeners(evt)
  }

  override fun onGuildVoiceSelfDeafen(evt: GuildVoiceSelfDeafenEvent) {
    runListeners(evt)
  }

  override fun onGuildVoiceSelfMute(evt: GuildVoiceSelfMuteEvent) {
    runListeners(evt)
  }

  override fun onGuildVoiceSuppress(evt: GuildVoiceSuppressEvent) {
    runListeners(evt)
  }
}