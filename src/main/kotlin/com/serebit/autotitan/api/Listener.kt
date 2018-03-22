package com.serebit.autotitan.api

import net.dv8tion.jda.core.events.Event
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import com.serebit.autotitan.api.annotations.Listener as ListenerAnnotation

internal class Listener(
    private val function: KFunction<Unit>,
    private val instance: Any
) {
    val eventType: KClass<out Any> = function.valueParameters[0].type.jvmErasure

    operator fun invoke(evt: Event) {
        if (evt::class == eventType) function.call(instance, evt)
    }
}
