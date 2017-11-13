package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.annotations.ListenerFunction
import net.dv8tion.jda.core.events.Event
import java.lang.reflect.Method

class Listener private constructor(private val instance: Any, private val method: Method, info: ListenerFunction) {
    val name = if (info.name.isEmpty()) method.name.toLowerCase() else info.name
    val description = info.description
    val eventType: Class<*> = method.parameterTypes[0]

    operator fun invoke(evt: Event) {
        if (evt::class.java == eventType) method.invoke(instance, evt)
    }

    companion object {
        internal fun generate(instance: Any, method: Method): Listener? {
            return if (!isValid(method)) null else Listener(
                    instance,
                    method,
                    method.getAnnotation(ListenerFunction::class.java)
            )
        }

        internal fun isValid(method: Method): Boolean {
            return if (method.parameterCount == 1) {
                val hasAnnotation = method.isAnnotationPresent(ListenerFunction::class.java)
                val hasValidParameter by lazy {
                    Event::class.java.isAssignableFrom(method.parameterTypes[0])
                }
                hasAnnotation && hasValidParameter
            } else false
        }
    }
}