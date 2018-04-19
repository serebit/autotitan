package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Descriptor
import com.serebit.autotitan.api.meta.Restrictions
import com.serebit.autotitan.config
import com.serebit.autotitan.data.Emote
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass

abstract class Module(val isOptional: Boolean = false) {
    var name: String = ""
        private set
    private val commands: MutableList<Command> = mutableListOf()
    private val listeners: MutableList<Listener> = mutableListOf()
    internal val commandListField
        get() = MessageEmbed.Field(
            name,
            commands.filter { !it.isHidden }.joinToString("\n") { it.summary },
            false
        )
    val isStandard get() = !isOptional

    init {
        run {
            name = this::class.simpleName ?: "Anonymous Module"
        }
    }

    protected fun addCommand(
        descriptor: Descriptor,
        restrictions: Restrictions = Restrictions(),
        delimitLastString: Boolean = true,
        parameterTypes: List<KClass<*>>,
        function: (MessageReceivedEvent, List<Any>) -> Unit
    ): Boolean = if (parameterTypes.all { it in validParameterTypes }) {
        commands.add(
            Command(
                descriptor,
                restrictions,
                delimitLastString,
                parameterTypes,
                function
            )
        )
    } else false

    protected inline fun command(
        name: String,
        description: String = "",
        restrictions: Restrictions = Restrictions(),
        delimitLastString: Boolean = true,
        crossinline task: (MessageReceivedEvent) -> Unit
    ) = addCommand(
        Descriptor(name.toLowerCase(), description), restrictions, delimitLastString, emptyList()
    ) { evt, _ -> task(evt) }

    protected inline fun <reified P0> command(
        name: String,
        description: String = "",
        restrictions: Restrictions = Restrictions(),
        delimitLastString: Boolean = true,
        crossinline task: (MessageReceivedEvent, P0) -> Unit
    ) = addCommand(
        Descriptor(name.toLowerCase(), description), restrictions, delimitLastString, listOf(P0::class)
    ) { evt, args ->
        task(evt, args[0] as P0)
    }

    protected inline fun <reified P0, reified P1> command(
        name: String,
        description: String = "",
        restrictions: Restrictions = Restrictions(),
        delimitLastString: Boolean = true,
        crossinline task: (MessageReceivedEvent, P0, P1) -> Unit
    ) = addCommand(
        Descriptor(name.toLowerCase(), description), restrictions, delimitLastString,
        listOf(P0::class, P1::class)
    ) { evt, args ->
        task(evt, args[0] as P0, args[1] as P1)
    }

    protected inline fun <reified P0, reified P1, reified P2> command(
        name: String,
        description: String = "",
        restrictions: Restrictions = Restrictions(),
        delimitLastString: Boolean = true,
        crossinline task: (MessageReceivedEvent, P0, P1, P2) -> Unit
    ) = addCommand(
        Descriptor(name.toLowerCase(), description), restrictions, delimitLastString,
        listOf(P0::class, P1::class, P2::class)
    ) { evt, args ->
        @Suppress("MagicNumber")
        task(evt, args as P0, args[1] as P1, args[2] as P2)
    }

    protected inline fun <reified P0, reified P1, reified P2, reified P3> command(
        name: String,
        description: String = "",
        restrictions: Restrictions = Restrictions(),
        delimitLastString: Boolean = true,
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3) -> Unit
    ) = addCommand(
        Descriptor(name.toLowerCase(), description), restrictions, delimitLastString,
        listOf(P0::class, P1::class, P2::class, P3::class)
    ) { evt, args ->
        @Suppress("MagicNumber")
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3)
    }

    protected inline fun <reified P0, reified P1, reified P2, reified P3, reified P4> command(
        name: String,
        description: String = "",
        restrictions: Restrictions = Restrictions(),
        delimitLastString: Boolean = true,
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3, P4) -> Unit
    ) = addCommand(
        Descriptor(name.toLowerCase(), description), restrictions, delimitLastString,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class)
    ) { evt, args ->
        @Suppress("MagicNumber")
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4)
    }

    protected inline fun <reified P0, reified P1, reified P2, reified P3, reified P4, reified P5> command(
        name: String,
        description: String = "",
        restrictions: Restrictions = Restrictions(),
        delimitLastString: Boolean = true,
        crossinline task: (MessageReceivedEvent, P0, P1, P2, P3, P4, P5) -> Unit
    ) = addCommand(
        Descriptor(name.toLowerCase(), description), restrictions, delimitLastString,
        listOf(P0::class, P1::class, P2::class, P3::class, P4::class, P5::class)
    ) { evt, args ->
        @Suppress("MagicNumber")
        task(evt, args[0] as P0, args[1] as P1, args[2] as P2, args[3] as P3, args[4] as P4, args[5] as P5)
    }

    protected fun addListener(eventType: KClass<out Event>, function: (Event) -> Unit) = listeners.add(
        Listener(eventType, function)
    )

    protected inline fun <reified T : Event> listener(crossinline task: (T) -> Unit) = addListener(T::class) {
        task(it as T)
    }

    fun getInvokeableCommandField(evt: MessageReceivedEvent): MessageEmbed.Field? {
        val validCommands = commands.filter { it.isVisibleFrom(evt) }
        return if (validCommands.isNotEmpty()) {
            MessageEmbed.Field(name, validCommands.joinToString("\n") { it.summary }, false)
        } else null
    }

    internal fun invoke(evt: Event) {
        listeners.asSequence()
            .filter { it.eventType == evt::class }
            .forEach { it.invoke(evt) }
        if (evt is MessageReceivedEvent && evt.message.contentRaw.startsWith(config.prefix)) {
            commands.asSequence()
                .filter { it.isInvokeableFrom(evt) }
                .associate { it to it.parseTokensOrNull(evt) }.entries
                .firstOrNull { it.value != null }?.let { (command, parameters) ->
                    command(evt, parameters!!)
                }

        }
    }

    internal fun findCommandsByName(name: String): List<Command> =
        commands.filter { it.matchesName(name) && !it.isHidden }

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
