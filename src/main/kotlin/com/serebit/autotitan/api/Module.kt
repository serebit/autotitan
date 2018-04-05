package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass
import com.serebit.autotitan.api.meta.annotations.Command as CommandAnnotation
import com.serebit.autotitan.api.meta.annotations.Listener as ListenerAnnotation

abstract class Module(name: String = "", val isOptional: Boolean = false) {
    var name: String = name
        private set
    private val commands = mutableListOf<Command>()
    private val listeners = mutableListOf<Listener>()
    val commandListField: MessageEmbed.Field
        get() = MessageEmbed.Field(
            name,
            commands.filter { it.isNotHidden }.joinToString("\n") { it.summary },
            false
        )
    private val validParameterTypes = setOf(
        Boolean::class,
        Byte::class,
        Short::class,
        Int::class,
        Long::class,
        Float::class,
        Double::class,
        User::class,
        Member::class,
        Channel::class,
        Char::class,
        String::class
    )
    val isStandard get() = !isOptional

    init {
        safeNameSet(name)
    }

    private fun safeNameSet(name: String) {
        this.name = if (name.isNotBlank()) name else this::class.simpleName ?: name
    }

    fun runCommands(evt: MessageReceivedEvent) {
        val (command, parameters) = commands.asSequence()
            .filter { it.looselyMatches(evt.message.contentRaw) }
            .associate { it to it.parseTokensOrNull(evt) }.entries
            .firstOrNull { it.value != null } ?: return
        command(evt, parameters!!)
    }

    fun runListeners(evt: Event) {
        listeners.filter { it.eventType == evt::class }.forEach { it(evt) }
    }

    fun findCommandsByName(name: String): List<Command>? = commands.filter { it.name == name }

    protected fun addCommand(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        parameterTypes: List<KClass<*>>,
        wrapper: FunctionWrapper
    ): Boolean = if (parameterTypes.all { it in validParameterTypes }) {
        commands.add(
            Command(
                name.toLowerCase(), description,
                access, locale,
                delimitLastString,
                hidden,
                permissions,
                parameterTypes,
                wrapper
            )
        )
    } else false

    protected fun command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        task: (MessageReceivedEvent) -> Unit
    ) = addCommand(name, description, access, locale, delimitLastString, hidden, permissions, emptyList(),
        object : FunctionWrapper {
            override fun invoke(evt: Event, vararg args: Any) {
                task(evt as MessageReceivedEvent)
            }
        }
    )

    protected inline fun <reified P0> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0) -> Unit
    ) = addCommand(name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class),
        object : FunctionWrapper {
            override fun invoke(evt: Event, vararg args: Any) {
                println(args[0]::class.qualifiedName)
                task(evt as MessageReceivedEvent, args[0] as P0)
            }
        }
    )

    protected inline fun <reified P0, reified P1> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1) -> Unit
    ) = addCommand(name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class),
        object : FunctionWrapper {
            override fun invoke(evt: Event, vararg args: Any) =
                task(evt as MessageReceivedEvent, args[0] as P0, args[1] as P1)
        }
    )

    protected inline fun <reified P0, reified P1, reified P2> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1, P2) -> Unit
    ) = addCommand(name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class, P2::class),
        object : FunctionWrapper {
            override fun invoke(evt: Event, vararg args: Any) =
                task(evt as MessageReceivedEvent, args[0] as P0, args[1] as P1, args[2] as P2)
        })

    protected inline fun <reified P0, reified P1, reified P2, reified P3> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3) -> Unit
    ) = addCommand(name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class, P2::class, P3::class),
        object : FunctionWrapper {
            override fun invoke(evt: Event, vararg args: Any) =
                task(evt as MessageReceivedEvent, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
        })

    protected inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3, P4) -> Unit
    ) = addCommand(name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class),
        object : FunctionWrapper {
            override fun invoke(evt: Event, vararg args: Any) = task(
                evt as MessageReceivedEvent,
                args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4
            )
        })

    protected inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3, P4, P5) -> Unit
    ) = addCommand(name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class, P5::class),
        object : FunctionWrapper {
            override fun invoke(evt: Event, vararg args: Any) = task(
                evt as MessageReceivedEvent,
                args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5
            )
        })

    protected inline fun <reified T : Event> listener(crossinline task: (T) -> Unit) {

    }
}
