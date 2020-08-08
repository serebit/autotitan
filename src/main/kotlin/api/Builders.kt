package com.serebit.autotitan.api

import com.serebit.autotitan.internal.ScriptContext
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.reflect.typeOf

inline fun defaultModule(
    name: String,
    defaultAccess: Access = Access.All(),
    init: ModuleTemplate.() -> Unit
) = ScriptContext.pushModule(ModuleTemplate(name, false, defaultAccess).apply(init))

inline fun optionalModule(
    name: String,
    defaultAccess: Access = Access.All(),
    init: ModuleTemplate.() -> Unit
) = ScriptContext.pushModule(ModuleTemplate(name, true, defaultAccess).apply(init))

inline fun ModuleTemplate.group(
    name: String,
    description: String = "",
    defaultAccess: Access = this.defaultAccess,
    init: GroupTemplate.() -> Unit
) = addGroup(GroupTemplate(name, description, defaultAccess).apply(init))

inline fun InvokeableContainerTemplate.suspendCommand(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.() -> Unit
) = addSuspendCommand(name, description, access, emptyList()) { evt, _ -> task(evt) }

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0> InvokeableContainerTemplate.suspendCommand(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0) -> Unit
) where P0 : Any = addSuspendCommand(name, description, access, listOf(typeOf<P0>())) { evt, args ->
    task(evt, args[0] as P0)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1> InvokeableContainerTemplate.suspendCommand(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1) -> Unit
) where P0 : Any, P1 : Any =
    addSuspendCommand(name, description, access, listOf(typeOf<P0>(), typeOf<P1>())) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1)
    }

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1, reified P2> InvokeableContainerTemplate.suspendCommand(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2) -> Unit
) where P0 : Any, P1 : Any, P2 : Any =
    addSuspendCommand(name, description, access, listOf(typeOf<P0>(), typeOf<P1>(), typeOf<P2>())) { evt, args ->
        task(evt, args as P0, args[1] as P1, args[2] as P2)
    }

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1, reified P2, reified P3> InvokeableContainerTemplate.suspendCommand(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any = addSuspendCommand(
    name, description, access,
    listOf(typeOf<P0>(), typeOf<P1>(), typeOf<P2>(), typeOf<P3>())
) { evt, args ->
    task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> InvokeableContainerTemplate.suspendCommand(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any = addSuspendCommand(
    name, description, access,
    listOf(typeOf<P0>(), typeOf<P1>(), typeOf<P2>(), typeOf<P3>(), typeOf<P4>())
) { evt, args ->
    task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> InvokeableContainerTemplate.suspendCommand(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: suspend MessageReceivedEvent.(P0, P1, P2, P3, P4, P5) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any, P5 : Any = addSuspendCommand(
    name, description, access,
    listOf(typeOf<P0>(), typeOf<P1>(), typeOf<P2>(), typeOf<P3>(), typeOf<P4>(), typeOf<P5>())
) { evt, args ->
    task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5)
}

inline fun InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: MessageReceivedEvent.() -> Unit
) = addCommand(name, description, access, emptyList()) { evt, _ -> task(evt) }

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: MessageReceivedEvent.(P0) -> Unit
) where P0 : Any = addCommand(name, description, access, listOf(typeOf<P0>())) { evt, args ->
    task(evt, args[0] as P0)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: MessageReceivedEvent.(P0, P1) -> Unit
) where P0 : Any, P1 : Any =
    addCommand(name, description, access, listOf(typeOf<P0>(), typeOf<P1>())) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1)
    }

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1, reified P2> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: MessageReceivedEvent.(P0, P1, P2) -> Unit
) where P0 : Any, P1 : Any, P2 : Any =
    addCommand(name, description, access, listOf(typeOf<P0>(), typeOf<P1>(), typeOf<P2>())) { evt, args ->
        task(evt, args as P0, args[1] as P1, args[2] as P2)
    }

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1, reified P2, reified P3> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: MessageReceivedEvent.(P0, P1, P2, P3) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any = addCommand(
    name, description, access,
    listOf(typeOf<P0>(), typeOf<P1>(), typeOf<P2>(), typeOf<P3>())
) { evt, args ->
    task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: MessageReceivedEvent.(P0, P1, P2, P3, P4) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any = addCommand(
    name, description, access,
    listOf(typeOf<P0>(), typeOf<P1>(), typeOf<P2>(), typeOf<P3>(), typeOf<P4>())
) { evt, args ->
    task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> InvokeableContainerTemplate.command(
    name: String, description: String = "",
    access: Access = defaultAccess,
    crossinline task: MessageReceivedEvent.(P0, P1, P2, P3, P4, P5) -> Unit
) where P0 : Any, P1 : Any, P2 : Any, P3 : Any, P4 : Any, P5 : Any = addCommand(
    name, description, access,
    listOf(typeOf<P0>(), typeOf<P1>(), typeOf<P2>(), typeOf<P3>(), typeOf<P4>(), typeOf<P5>())
) { evt, args ->
    task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5)
}

inline fun <reified T : GenericEvent> ModuleTemplate.listener(crossinline task: T.() -> Unit) =
    addListener { if (it is T) it.task() }

inline fun <reified T : GenericEvent> ModuleTemplate.suspendListener(crossinline task: suspend T.() -> Unit) =
    addSuspendListener { if (it is T) it.task() }
