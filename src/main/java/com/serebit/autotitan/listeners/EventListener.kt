package com.serebit.autotitan.listeners

import com.serebit.autotitan.config.Configuration
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Listener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class EventListener(
        private val commands: Set<Command>,
        private val listeners: Set<Listener>
) : ListenerAdapter() {
    override fun onGenericEvent(evt: Event) {
        launch(CommonPool) {
            if (evt is MessageReceivedEvent && evt.message.rawContent.startsWith(Configuration.prefix)) {
                runCommands(evt)
            }
            runListeners(evt)
        }
    }

    private fun runListeners(evt: Event) {
        listeners.filter { it.eventType == evt::class.java }.forEach {
            it(evt)
        }
    }

    private fun runCommands(evt: MessageReceivedEvent) {
        if (evt.message.rawContent == "${Configuration.prefix}help") sendCommandList(evt)
        var parameters: List<Any>? = null
        val command = commands
                .asSequence()
                .filter {
                    it.looselyMatches(evt.message.rawContent)
                }
                .filter {
                    parameters = it.castParametersOrNull(evt)
                    parameters != null
                }
                .firstOrNull()
        if (command != null && parameters != null) {
            command(evt, parameters as List<Any>)
        }
    }

    private fun sendCommandList(evt: MessageReceivedEvent) {
        val embed = EmbedBuilder().apply {
            setColor(evt.guild?.selfMember?.color)
            commands.sortedBy { it.name }.groupBy { it.method.declaringClass }.forEach {
                addField(it.key.simpleName, it.value.joinToString("\n") {
                    it.helpMessage
                }, false)
            }
        }.build()

        evt.channel.sendMessage(embed).queue()
    }
}