package com.serebit.autotitan.api

import net.dv8tion.jda.core.events.Event
import java.lang.reflect.Method
import com.serebit.autotitan.api.meta.annotations.Listener as ListenerAnnotation

class Listener private constructor(private val instance: Any, private val method: Method) {
    val eventType: Class<out Any> = method.parameterTypes[0]

    operator fun invoke(evt: Event) {
        if (evt::class.java == eventType) method.invoke(instance, evt)
    }

    companion object {
        fun generate(instance: Any, method: Method): Listener? {
            if (!isValid(method)) return null
            return Listener(
                    instance,
                    method
            )
        }

        fun isValid(method: Method): Boolean {
            if (method.parameterCount != 1) return false
            if (!method.isAnnotationPresent(ListenerAnnotation::class.java)) return false
            return Event::class.java.isAssignableFrom(method.parameterTypes[0])
        }
    }
}