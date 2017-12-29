package com.serebit.autotitan.api

import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import com.serebit.autotitan.api.meta.annotations.Command as CommandAnnotation
import com.serebit.autotitan.api.meta.annotations.Listener as ListenerAnnotation

abstract class Module(name: String = "", val isOptional: Boolean = false) {
    var name: String = name
        private set
    private val commands: MutableList<Command> = mutableListOf()
    private val listeners: MutableList<Listener> = mutableListOf()
    private val validParameterTypes = setOf(
            Int::class,
            Long::class,
            Double::class,
            Float::class,
            Boolean::class,
            User::class,
            Member::class,
            Channel::class,
            String::class
    )
    lateinit var commandListField: MessageEmbed.Field
    val isStandard get() = !isOptional

    fun init() {
        this::class.declaredMemberFunctions.forEach { addFunction(it) }
        name = if (name.isNotBlank()) name else this::class.simpleName ?: name
        commandListField = MessageEmbed.Field(name, commands.joinToString("\n") { it.summary }, false)
    }

    fun runListeners(evt: Event) {
        listeners.filter { it.eventType == evt::class }.forEach { it.invoke(evt) }
    }

    fun runCommands(evt: MessageReceivedEvent) {
        val (command, parameters) = commands.asSequence()
                .filter { it.looselyMatches(evt.message.contentRaw) }
                .associate { it to it.parseTokensOrNull(evt) }.entries
                .firstOrNull { it.value != null } ?: return
        command(evt, parameters!!)
    }

    private fun <T> addFunction(function: KFunction<T>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val specificFunction = if (function.returnType.jvmErasure == Unit::class) {
            function as KFunction<Unit>
        } else return false
        return when {
            specificFunction.isValidCommand -> {
                val annotation = specificFunction.findAnnotation<CommandAnnotation>() ?: return false
                commands.add(Command(
                        specificFunction,
                        this,
                        (if (annotation.name.isNotBlank()) annotation.name else specificFunction.name).toLowerCase(),
                        annotation.description.trim(),
                        annotation.access,
                        annotation.locale,
                        annotation.splitLastParameter,
                        annotation.hidden,
                        annotation.memberPermissions.toList()
                ))
            }
            specificFunction.isValidListener -> {
                listeners.add(Listener(
                        function,
                        this
                ))
            }
            else -> false
        }
    }

    fun findCommandByName(name: String): Command? = commands.find { it.name == name }

    private val KFunction<Unit>.isValidListener: Boolean
        get() {
            if (valueParameters.size != 1) return false
            if (findAnnotation<ListenerAnnotation>() == null) return false
            return valueParameters[0].type.jvmErasure.isSubclassOf(Event::class)
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
                        it.type.jvmErasure in validParameterTypes
                    }
        }
}