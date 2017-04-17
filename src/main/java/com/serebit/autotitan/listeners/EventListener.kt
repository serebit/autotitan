package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Listener
import net.dv8tion.jda.core.events.Event

interface EventListener {
  val listeners: MutableSet<Listener>
  val validEventTypes: MutableSet<Class<out Event>>
  fun runListeners(evt: Event)
}