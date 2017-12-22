package com.serebit.autotitan.listeners

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.config
import com.serebit.extensions.jda.sendEmbed
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class EventListener(
        private val modules: List<Module>
) : ListenerAdapter() {
    override fun onGenericEvent(evt: Event) {
        launch {
            modules.forEach { module ->
                module.listeners.filter { it.eventType == evt::class.java }.forEach {
                    it(evt)
                }
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
        modules.forEach {
            val (command, parameters) = it.commands.asSequence()
                    .filter { it.looselyMatches(evt.message.rawContent) }
                    .associate { it to it.parseTokensOrNull(evt) }.entries
                    .firstOrNull { it.value != null } ?: return
            command(evt, parameters!!)
        }
    }

    private fun sendCommandList(evt: MessageReceivedEvent) {
        evt.run {
            channel.sendEmbed {
                setColor(guild?.selfMember?.color)
                modules.forEach { module ->
                    addField(
                            module::class.simpleName,
                            module.commands.joinToString("\n") { it.helpMessage },
                            false
                    )
                }
            }.complete()
        }
    }
}