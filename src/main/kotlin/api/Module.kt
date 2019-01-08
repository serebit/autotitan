package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.config
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

internal data class Module(
    val name: String,
    val isOptional: Boolean,
    private val commands: List<Command>, private val listeners: List<Listener>,
    private val dataManager: DataManager
) {
    val commandListField
        get() = MessageEmbed.Field(
            name,
            commands.filter { !it.isHidden }.joinToString { it.summary },
            false
        )
    val isStandard get() = !isOptional

    fun getInvokeableCommandField(evt: MessageReceivedEvent): MessageEmbed.Field? {
        val validCommands = commands.filter { it.isVisibleFrom(evt) }
        return if (validCommands.isNotEmpty()) {
            MessageEmbed.Field(name, validCommands.joinToString { it.summary }, false)
        } else null
    }

    suspend fun invoke(evt: Event) {
        listeners.asSequence()
            .filter { it.eventType.isInstance(evt) }
            .forEach { it(evt) }
        if (evt is MessageReceivedEvent && evt.message.contentRaw.startsWith(config.prefix)) {
            commands.asSequence()
                .filter { it.isInvokeableFrom(evt) }
                .associate { it to it.parseTokensOrNull(evt) }.entries
                .find { it.value != null }?.let { (command, parameters) ->
                    command(evt, parameters!!)
                }
        }
    }

    fun findCommandsByName(name: String): List<Command> = commands.filter { it.matchesName(name) && !it.isHidden }
}

fun module(
    name: String,
    isOptional: Boolean = false,
    defaultAccess: Access = Access.All(),
    init: ModuleTemplate.() -> Unit
) = ModuleTemplate(name, isOptional, defaultAccess).apply(init)
