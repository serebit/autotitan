package com.serebit.autotitan.api

import net.dv8tion.jda.core.events.Event
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import com.serebit.autotitan.api.meta.annotations.Listener as ListenerAnnotation

class Listener private constructor(
        private val function: KFunction<Unit>,
        private val instance: Any
) {
    val eventType: KClass<out Any> = function.valueParameters[0].type.jvmErasure

    operator fun invoke(evt: Event) {
        if (evt::class == eventType) function.call(instance, evt)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun generate(function: KFunction<*>, instance: Any): Listener? = if (isValid(function)) Listener(
                function as KFunction<Unit>,
                instance
        ) else null

        fun isValid(function: KFunction<*>): Boolean {
            if (function.valueParameters.size != 1) return false
            if (function.findAnnotation<ListenerAnnotation>() == null) return false
            return function.valueParameters[0].type.jvmErasure.isSubclassOf(Event::class)
        }
    }
}