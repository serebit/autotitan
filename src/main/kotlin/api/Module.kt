package com.serebit.autotitan.api

import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
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
        this::class.declaredMemberFunctions.forEach { addFunction(it) }
        name = if (name.isNotBlank()) name else this::class.simpleName ?: name
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

    internal fun runListeners(evt: GenericEvent) {
        listeners.filter { it.eventType == evt::class }.forEach { it.invoke(evt) }
    }

    internal fun runCommands(evt: MessageReceivedEvent) {
        val (command, parameters) = commands.asSequence()
            .filter { it.looselyMatches(evt.message.contentRaw) }
            .associate { it to it.parseTokensOrNull(evt) }.entries
            .firstOrNull { it.value != null } ?: return
        command(this, evt, parameters!!)
    }

    private fun <T> addFunction(function: KFunction<T>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val specificFunction = if (function.returnType.jvmErasure == Unit::class) {
            function as KFunction<Unit>
        } else return false
        return when {
            specificFunction.isValidCommand -> {
                // we know it isn't null, because isValidCommand checks for a null annotation
                val annotation = specificFunction.findAnnotation<CommandAnnotation>()!!
                commands.add(
                    Command(
                        specificFunction,
                        (if (annotation.name.isNotBlank()) annotation.name else specificFunction.name).toLowerCase(),
                        annotation.description.trim(),
                        annotation.access,
                        annotation.locale,
                        annotation.splitLastParameter,
                        annotation.hidden,
                        annotation.memberPermissions.toList()
                    )
                )
            }
            specificFunction.isValidListener -> listeners.add(Listener(function, this))
            else -> false
        }
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
}

interface ModuleCompanion {
    fun provide(): Module
}
