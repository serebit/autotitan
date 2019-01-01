package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Descriptor
import com.serebit.autotitan.api.parser.TokenType
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass

open class ModuleTemplate(
    val name: String = "GenericModule",
    val isOptional: Boolean = false,
    inline val defaultAccess: Access = Access.All()
) {
    val dataManager = DataManager(name)
    private val commands: MutableList<Command> = mutableListOf()
    private val listeners: MutableList<Listener> = mutableListOf()

    fun addCommand(
        name: String,
        description: String,
        access: Access = defaultAccess,
        parameterTypes: List<KClass<*>> = emptyList(),
        function: suspend MessageReceivedEvent.(List<Any>) -> Unit
    ): Boolean = parameterTypes.map { TokenType.from(it) }.let { tokenTypes ->
        if (tokenTypes.none { it == null }) commands.add(
            Command(
                Descriptor(name.toLowerCase(), description),
                access,
                tokenTypes.requireNoNulls(),
                function
            )
        ) else false
    }

    inline fun command(
        name: String, description: String = "",
        access: Access = defaultAccess,
        crossinline task: suspend MessageReceivedEvent.() -> Unit
    ) = addCommand(name, description, access, emptyList()) { task() }

    inline fun <reified P0> command(
        name: String, description: String = "",
        access: Access = defaultAccess,
        crossinline task: suspend MessageReceivedEvent.(P0) -> Unit
    ) = addCommand(name, description, access, listOf(P0::class)) { args ->
        task(args[0] as P0)
    }

    inline fun <reified P0, reified P1> command(
        name: String, description: String = "",
        access: Access = defaultAccess,
        crossinline task: suspend MessageReceivedEvent.(P0, P1) -> Unit
    ) = addCommand(name, description, access, listOf(P0::class, P1::class)) { args ->
        task(args[0] as P0, args[1] as P1)
    }

    inline fun <reified P0, reified P1, reified P2> command(
        name: String, description: String = "",
        access: Access = defaultAccess,
        crossinline task: suspend MessageReceivedEvent.(P0, P1, P2) -> Unit
    ) = addCommand(name, description, access, listOf(P0::class, P1::class, P2::class)) { args ->
        task(args as P0, args[1] as P1, args[2] as P2)
    }

    inline fun <reified P0, reified P1, reified P2, reified P3> command(
        name: String, description: String = "",
        access: Access = defaultAccess,
        crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3) -> Unit
    ) = addCommand(name, description, access, listOf(P0::class, P1::class, P2::class, P3::class)) { args ->
        task(args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
    }

    inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> command(
        name: String, description: String = "",
        access: Access = defaultAccess,
        crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4) -> Unit
    ) = addCommand(
        name, description, access, listOf(P0::class, P1::class, P2::class, P3::class, P4::class)
    ) { args ->
        task(args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4)
    }

    inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> command(
        name: String, description: String = "",
        access: Access = defaultAccess,
        crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4, P5) -> Unit
    ) = addCommand(
        name, description, access,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class, P5::class)
    ) { args ->
        task(args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5)
    }

    fun addListener(eventType: KClass<out Event>, function: (Event) -> Unit) =
        listeners.add(Listener(eventType, function))

    inline fun <reified T : Event> listener(crossinline task: T.() -> Unit) =
        addListener(T::class) { (it as T).task() }

    internal fun build() = Module(name, isOptional, commands, listeners, dataManager)
}
