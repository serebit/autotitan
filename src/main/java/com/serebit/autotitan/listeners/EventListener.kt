package com.serebit.autotitan.listeners

import com.serebit.autotitan.api.Command
import com.serebit.autotitan.api.Listener
import com.serebit.autotitan.api.meta.annotations.Module
import com.serebit.autotitan.config
import com.serebit.extensions.jda.sendEmbed
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure
import com.serebit.autotitan.api.meta.annotations.Command as CommandAnnotation

class EventListener(
        commands: Collection<Command>,
        listeners: Collection<Listener>
) : ListenerAdapter() {
    private val commands = commands.toMutableList()
    private val listeners = listeners.toMutableList()

    init {
        Help().let { helpModule ->
            this.commands.addAll(
                    helpModule::class.declaredMemberFunctions.mapNotNull { Command.generate(it, helpModule) }
            )
        }

    }

    override fun onGenericEvent(evt: Event) {
        launch {
            listeners.filter { it.eventType == evt::class }.forEach {
                it(evt)
            }
        }
    }

    override fun onMessageReceived(evt: MessageReceivedEvent) {
        launch {
            if (evt.message.rawContent.startsWith(config.prefix)) {
                runCommands(evt)
            }
        }
    }

    private fun runCommands(evt: MessageReceivedEvent) {
        val (command, parameters) = commands.asSequence()
                .filter { it.looselyMatches(evt.message.rawContent) }
                .associate { it to it.parseTokensOrNull(evt) }.entries
                .firstOrNull { it.value != null } ?: return
        command(evt, parameters!!)
    }

    @Module
    inner class Help {
        @CommandAnnotation
        fun help(evt: MessageReceivedEvent) {
            evt.run {
                channel.sendEmbed {
                    setColor(guild?.selfMember?.color)
                    commands.sortedBy { it.name }
                            .groupBy { it.function.instanceParameter?.type?.jvmErasure?.simpleName }.entries
                            .sortedBy { it.key }
                            .forEach { (extension, commands) ->
                                addField(extension, commands.joinToString("\n") { it.summary }, false)
                            }
                }.complete()
            }
        }

        @CommandAnnotation
        fun help(evt: MessageReceivedEvent, commandName: String) {
            commands.firstOrNull { it.name == commandName }?.let { command ->
                evt.channel.sendMessage(command.helpEmbed).complete()
            }
        }
    }
}