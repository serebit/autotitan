package com.serebit.autotitan.listeners

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.config
import com.serebit.extensions.jda.sendEmbed
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import com.serebit.autotitan.api.meta.annotations.Command as CommandAnnotation

class EventListener(
        modules: List<Module>
) : ListenerAdapter() {
    private val modules = modules.toMutableList()

    init {
        this.modules.add(Help())
    }

    override fun onGenericEvent(evt: Event) {
        launch {
            modules.forEach { module ->
                module.listeners.filter { it.eventType == evt::class }.forEach { listener ->
                    listener(evt)
                }
            }
        }
    }

    override fun onMessageReceived(evt: MessageReceivedEvent) {
        launch {
            if (evt.message.contentRaw.startsWith(config.prefix)) {
                runCommands(evt)
            }
        }
    }

    private fun runCommands(evt: MessageReceivedEvent) {
        modules.forEach { module ->
            val (command, parameters) = module.commands.asSequence()
                    .filter { it.looselyMatches(evt.message.contentRaw) }
                    .associate { it to it.parseTokensOrNull(evt) }.entries
                    .firstOrNull { it.value != null } ?: return@forEach
            command(evt, parameters!!)
        }
    }

    inner class Help : Module() {
        @CommandAnnotation(description = "Sends an embed with a list of commands.")
        fun help(evt: MessageReceivedEvent) {
            evt.run {
                channel.sendEmbed {
                    setColor(guild?.selfMember?.color)
                    modules.sortedBy { it.name }
                            .forEach { module ->
                                addField(module.name, module.commands.joinToString("\n") { it.summary }, false)
                            }
                }.complete()
            }
        }

        @CommandAnnotation(description = "Sends an embed with information about the requested command.")
        fun help(evt: MessageReceivedEvent, commandName: String) {
            val matchingCommands = modules.map { it.commands }.flatten().filter { it.name == commandName }
            if (matchingCommands.isNotEmpty()) evt.channel.sendEmbed {
                setColor(evt.guild?.selfMember?.color)
                matchingCommands.forEach { command ->
                    addField(command.helpField)
                }
            }.complete()
        }
    }
}