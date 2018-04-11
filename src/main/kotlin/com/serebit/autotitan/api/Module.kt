package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.data.Emote
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import com.serebit.autotitan.api.annotations.Command as CommandAnnotation
import com.serebit.autotitan.api.annotations.Listener as ListenerAnnotation

abstract class Module(name: String = "", val isOptional: Boolean = false) {
    var name: String = name
        private set
    private val commands: MutableList<Command> = mutableListOf()
    private val listeners: MutableList<Listener> = mutableListOf()
    val commandListField
        get() = MessageEmbed.Field(
            name,
            commands.filter { it.isNotHidden }.joinToString("\n") { it.summary },
            false
        )
    val isStandard get() = !isOptional

    init {
        init()
    }

    private fun init() {
        name = if (name.isNotBlank()) name else this::class.simpleName ?: name
    }

    protected fun addCommand(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        parameterTypes: List<KClass<*>>,
        function: (MessageReceivedEvent, Array<out Any>) -> Unit
    ): Boolean = if (parameterTypes.all { it in validParameterTypes }) {
        commands.add(
            Command(
                name.toLowerCase(), description,
                access, locale,
                delimitLastString,
                hidden,
                permissions,
                parameterTypes,
                function
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
    ) = addCommand(
        name, description, access, locale, delimitLastString, hidden, permissions,
        emptyList()
    ) { evt, _ -> task(evt) }

    protected inline fun <reified P0> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0) -> Unit
    ) = addCommand(
        name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class)
    ) { evt, args ->
        task(evt, args[0] as P0)
    }

    protected inline fun <reified P0, reified P1> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1) -> Unit
    ) = addCommand(
        name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1)
    }

    protected inline fun <reified P0, reified P1, reified P2> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1, P2) -> Unit
    ) = addCommand(
        name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class, P2::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2)
    }

    protected inline fun <reified P0, reified P1, reified P2, reified P3> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3) -> Unit
    ) = addCommand(
        name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class, P2::class, P3::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
    }

    protected inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3, P4) -> Unit
    ) = addCommand(
        name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4)
    }

    protected inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> command(
        name: String,
        description: String = "",
        access: Access = Access.ALL,
        locale: Locale = Locale.ALL,
        delimitLastString: Boolean = true,
        hidden: Boolean = false,
        permissions: List<Permission> = emptyList(),
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3, P4, P5) -> Unit
    ) = addCommand(
        name, description, access, locale, delimitLastString, hidden, permissions,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class, P5::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5)
    }

    protected inline fun <reified T : Event> listener(crossinline task: (T) -> Unit) {

    }

    fun getInvokeableCommandList(evt: MessageReceivedEvent): MessageEmbed.Field? {
        val validCommands = commands.filter { it.isNotHidden && it.isInvokeableByAuthor(evt) }
        return if (validCommands.isNotEmpty()) {
            MessageEmbed.Field(
                name,
                validCommands.joinToString("\n") { it.summary },
                false
            )
        } else null
    }

    internal fun runListeners(evt: Event) {
        listeners.filter { it.eventType == evt::class }.forEach { it.invoke(evt) }
    }

    internal fun runCommands(evt: MessageReceivedEvent) {
        val (command, parameters) = commands.asSequence()
            .filter { it.looselyMatches(evt.message.contentRaw) }
            .associate { it to it.parseTokensOrNull(evt) }.entries
            .firstOrNull { it.value != null } ?: return
        command(evt, parameters!!)
    }

    internal fun findCommandsByName(name: String): List<Command>? = commands.filter { it.name == name }

    private val KFunction<Unit>.isValidListener: Boolean
        get() {
            return if (valueParameters.size != 1 || findAnnotation<ListenerAnnotation>() == null) false
            else valueParameters[0].type.jvmErasure.isSubclassOf(Event::class)
        }

    private val KFunction<Unit>.isValidCommand: Boolean
        get() {
            if (returnType.jvmErasure != Unit::class) return false
            if (valueParameters.isEmpty()) return false
            if (findAnnotation<CommandAnnotation>() == null) return false
            if (valueParameters[0].type.jvmErasure != MessageReceivedEvent::class) return false
            return valueParameters
                .drop(1)
                .all {
                    it.type.jvmErasure in Command.validParameterTypes
                }
        }

    companion object {
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
            Emote::class,
            Char::class,
            String::class
        )
    }
}
