package com.serebit.autotitan.api

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

fun module(
    name: String,
    isOptional: Boolean = false,
    defaultAccess: Access = Access.All(),
    init: ModuleTemplate.() -> Unit
) = ModuleTemplate(name, isOptional, defaultAccess).apply(init)

inline fun ModuleTemplate.group(
    name: String,
    description: String = "",
    defaultAccess: Access = this.defaultAccess,
    init: GroupTemplate.() -> Unit
) = addGroup(GroupTemplate(name, description, defaultAccess).apply(init))

inline fun InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.() -> Unit
) = addCommand(CommandTemplate(name, description, access, emptyList()) { evt, _ -> task(evt) })

inline fun <reified P0> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0) -> Unit
) where P0 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class)) { evt, args -> task(evt, args[0] as P0) })

inline fun <reified P0, reified P1> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1) -> Unit
) where P0 : Any, P1 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class, P1::class)) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1)
    })

inline fun <reified P0, reified P1, reified P2> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2) -> Unit
) where P0 : Any, P1 : Any, P2 : Any =
    addCommand(CommandTemplate(name, description, access, listOf(P0::class, P1::class, P2::class)) { evt, args ->
        task(evt, args as P0, args[1] as P1, args[2] as P2)
    })

inline fun <reified P0, reified P1, reified P2, reified P3> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any = addCommand(
    CommandTemplate(
        name, description, access,
        listOf(P0::class, P1::class, P2::class, P3::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
    }
)

inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any = addCommand(
    CommandTemplate(
        name, description, access,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4)
    }
)

inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4, P5) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any, P5 : Any = addCommand(
    CommandTemplate(
        name, description, access,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class, P5::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5)
    }
)

inline fun <reified T : Event> ModuleTemplate.listener(crossinline task: T.() -> Unit) =
    addListener(T::class) { (it as T).task() }
