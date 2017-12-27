package com.serebit.autotitan.listeners

import com.serebit.autotitan.api.Command
import com.serebit.autotitan.api.Listener
import com.serebit.autotitan.config
import com.serebit.extensions.jda.sendEmbed
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure

class EventListener(
        private val commands: Collection<Command>,
        private val listeners: Collection<Listener>
) : ListenerAdapter() {
    override fun onGenericEvent(evt: Event) {
        launch {
            listeners.filter { it.eventType == evt::class.java }.forEach {
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
        if (evt.message.rawContent == "${config.prefix}help") {
            sendCommandList(evt)
            return
        }
        val (command, parameters) = commands.asSequence()
                .filter { it.looselyMatches(evt.message.rawContent) }
                .associate { it to it.parseTokensOrNull(evt) }.entries
                .firstOrNull { it.value != null } ?: return
        command(evt, parameters!!)
    }

    private fun sendCommandList(evt: MessageReceivedEvent) {
        evt.run {
            channel.sendEmbed {
                setColor(guild?.selfMember?.color)
                commands.sortedBy { it.name }
                        .groupBy { it.function.instanceParameter?.type?.jvmErasure?.simpleName }.entries
                        .sortedBy { it.key }
                        .forEach { (extension, commands) ->
                            addField(extension, commands.joinToString("\n") { it.helpMessage }, false)
                        }
            }.complete()
        }
    }
}