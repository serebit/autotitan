package com.serebit.autotitan.listeners

import com.serebit.autotitan.config
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
            if (evt is MessageReceivedEvent && evt.message.rawContent.startsWith(config.prefix)) {
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