package com.serebit.autotitan.data

import com.serebit.autotitan.api.annotations.ListenerFunction
import net.dv8tion.jda.core.events.Event
import java.lang.reflect.Method

class Listener(val instance: Any, val method: Method, info: ListenerFunction) {
    val name = when (info.name) {
        "" -> method.name.toLowerCase()
        else -> info.name
    }
    val description = info.description
    val eventType: Class<*> = method.parameterTypes[0]

    companion object {
        fun isValid(method: Method): Boolean {
            return if (method.parameterTypes.isNotEmpty()) {
                val hasAnnotation = method.isAnnotationPresent(ListenerFunction::class.java)
                val hasValidParameter = Event::class.java.isAssignableFrom(method.parameterTypes[0])
                hasAnnotation && hasValidParameter
            } else false
        }
    }
}