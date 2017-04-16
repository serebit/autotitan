package com.serebit.autotitan.listeners

import net.dv8tion.jda.core.events.Event

interface EventListener {
  fun runListeners(evt: Event)
}