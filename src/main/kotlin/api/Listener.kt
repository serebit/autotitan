package com.serebit.autotitan.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.events.Event
import kotlin.reflect.KClass

internal data class Listener(
    val eventType: KClass<out Event>,
    private val function: suspend (Event) -> Unit
) : CoroutineScope {
    override val coroutineContext = Dispatchers.Main

    operator fun invoke(evt: Event) = launch { function(evt) }
}
