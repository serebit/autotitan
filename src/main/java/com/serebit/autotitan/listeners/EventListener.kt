package com.serebit.autotitan.listeners

import com.serebit.autotitan.data.Listener
import net.dv8tion.jda.core.events.Event

interface EventListener {
  val listeners: MutableList<Listener>
  fun runListeners(evt: Event)
}