package annotations

import net.dv8tion.jda.core.events.Event
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class ListenerFunction(
    val name: String = "",
    val description: String = "",
    val eventType: KClass<out Event>,
    val serverOnly: Boolean = false
)
