package com.serebit.autotitan.api

import net.dv8tion.jda.core.events.Event
import kotlin.reflect.KClass
import com.serebit.autotitan.api.annotations.Listener as ListenerAnnotation

internal class Listener(
    val eventType: KClass<out Event>,
    private val function: (Event) -> Unit
) {
    operator fun invoke(evt: Event) = function(evt)
}
