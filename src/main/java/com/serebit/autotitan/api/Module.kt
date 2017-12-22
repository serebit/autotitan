package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.reflect

abstract class Module {
    val commands = mutableListOf<Command>()
    val listeners = mutableListOf<Listener>()

    private fun addCommand(
            function: KFunction<Unit>?,
            name: String,
            description: String = "",
            access: Access = Access.ALL,
            locale: Locale = Locale.ALL,
            delimitLastString: Boolean = true,
            hidden: Boolean = false,
            permissions: List<Permission> = emptyList()
    ): Boolean {
        if (function == null) return false
        return commands.add(
                Command(function, name, description, access, locale, delimitLastString, hidden, permissions)
        )
    }

    protected fun command(
            name: String,
            description: String = "",
            access: Access = Access.ALL,
            locale: Locale = Locale.ALL,
            delimitLastString: Boolean = true,
            hidden: Boolean = false,
            permissions: List<Permission> = emptyList(),
            task: (MessageReceivedEvent) -> Unit
    ) = addCommand(task.reflect(), name, description, access, locale, delimitLastString, hidden, permissions)

    protected fun <P0> command(
            name: String,
            description: String = "",
            access: Access = Access.ALL,
            locale: Locale = Locale.ALL,
            delimitLastString: Boolean = true,
            hidden: Boolean = false,
            permissions: List<Permission> = emptyList(),
            task: (MessageReceivedEvent, P0) -> Unit
    ) = addCommand(task.reflect(), name, description, access, locale, delimitLastString, hidden, permissions)

    protected fun <P0, P1> command(
            name: String,
            description: String = "",
            access: Access = Access.ALL,
            locale: Locale = Locale.ALL,
            delimitLastString: Boolean = true,
            hidden: Boolean = false,
            permissions: List<Permission> = emptyList(),
            task: (MessageReceivedEvent, P0, P1) -> Unit
    ) = addCommand(task.reflect(), name, description, access, locale, delimitLastString, hidden, permissions)

    protected fun <P0, P1, P2> command(
            name: String,
            description: String = "",
            access: Access = Access.ALL,
            locale: Locale = Locale.ALL,
            delimitLastString: Boolean = true,
            hidden: Boolean = false,
            permissions: List<Permission> = emptyList(),
            task: (MessageReceivedEvent, P0, P1, P2) -> Unit
    ) = addCommand(task.reflect(), name, description, access, locale, delimitLastString, hidden, permissions)

    protected fun <P0, P1, P2, P3> command(
            name: String,
            description: String = "",
            access: Access = Access.ALL,
            locale: Locale = Locale.ALL,
            delimitLastString: Boolean = true,
            hidden: Boolean = false,
            permissions: List<Permission> = emptyList(),
            task: (MessageReceivedEvent, P0, P1, P2, P3) -> Unit
    ) = addCommand(task.reflect(), name, description, access, locale, delimitLastString, hidden, permissions)

    protected fun <P0, P1, P2, P3, P4> command(
            name: String,
            description: String = "",
            access: Access = Access.ALL,
            locale: Locale = Locale.ALL,
            delimitLastString: Boolean = true,
            hidden: Boolean = false,
            permissions: List<Permission> = emptyList(),
            task: (MessageReceivedEvent, P0, P1, P2, P3, P4) -> Unit
    ) = addCommand(task.reflect(), name, description, access, locale, delimitLastString, hidden, permissions)

    protected fun <P0, P1, P2, P3, P4, P5> command(
            name: String,
            description: String = "",
            access: Access = Access.ALL,
            locale: Locale = Locale.ALL,
            delimitLastString: Boolean = true,
            hidden: Boolean = false,
            permissions: List<Permission> = emptyList(),
            task: (MessageReceivedEvent, P0, P1, P2, P3, P4, P5) -> Unit
    ) = addCommand(task.reflect(), name, description, access, locale, delimitLastString, hidden, permissions)

    protected fun <T : Event> listener(task: (T) -> Unit) {

    }
}