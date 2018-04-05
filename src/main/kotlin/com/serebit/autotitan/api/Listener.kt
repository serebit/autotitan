package com.serebit.autotitan.api

import net.dv8tion.jda.core.events.Event
import kotlin.reflect.KClass

class Listener(
    private val function: FunctionWrapper,
    internal val eventType: KClass<out Event>
) {
    operator fun invoke(evt: Event) {
        if (evt::class == eventType) function(evt)
    }
}
