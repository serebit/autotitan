package com.serebit.autotitan.api

import net.dv8tion.jda.api.events.GenericEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

internal class Listener(
    private val function: KFunction<Unit>,
    private val instance: Any
) {
    val eventType: KClass<out Any> = function.valueParameters[0].type.jvmErasure

    operator fun invoke(evt: GenericEvent) {
        if (evt::class == eventType) function.call(instance, evt)
    }
}
