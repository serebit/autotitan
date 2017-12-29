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
    private val allModules: List<Module>
    private val standardModules: List<Module>
    private val optionalModules: List<Module>
    private val loadedModules get() = if (config.optionalsEnabled) allModules else standardModules

    init {
        allModules = modules.toMutableList().apply { add(Help().apply(Module::init)) }.toList()
        standardModules = allModules.filter { it.isStandard }
        optionalModules = allModules.filter { it.isOptional }
    }

    override fun onGenericEvent(evt: Event) {
        launch {
            loadedModules.forEach { it.runListeners(evt) }
        }
    }

    override fun onMessageReceived(evt: MessageReceivedEvent) {
        launch {
            if (evt.message.contentRaw.startsWith(config.prefix)) {
                loadedModules.forEach { it.runCommands(evt) }
            }
        }
    }

    inner class Help : Module() {
        @CommandAnnotation(description = "Sends an embed with a list of commands.")
        fun help(evt: MessageReceivedEvent) {
            evt.run {
                channel.sendEmbed {
                    setColor(guild?.selfMember?.color)
                    loadedModules.sortedBy { it.name }.forEach { module ->
                        addField(module.commandListField)
                    }
                }.complete()
            }
        }

        @CommandAnnotation(description = "Sends an embed with information about the requested command.")
        fun help(evt: MessageReceivedEvent, commandName: String) {
            val matchingCommands = loadedModules
                    .mapNotNull { it.findCommandByName(commandName) }
                    .filter { it.isNotHidden }
            if (matchingCommands.isNotEmpty()) {
                evt.channel.sendEmbed {
                    setColor(evt.guild?.selfMember?.color)
                    matchingCommands.forEach { command ->
                        addField(command.helpField)
                    }
                }.complete()
            } else {
                evt.channel.sendMessage("Could not find any commands matching `$commandName`.").complete()
            }
        }
    }
}