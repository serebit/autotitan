package com.serebit.autotitan.listeners

import com.serebit.autotitan.Logger
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.ModuleCompanion
import com.serebit.autotitan.config
import com.serebit.autotitan.extensions.sendEmbed
import com.serebit.autotitan.modules.*
import com.serebit.logkat.info
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import com.serebit.autotitan.api.annotations.Command as CommandAnnotation

internal object EventListener : ListenerAdapter() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val moduleProviders =
        listOf(Audio, AutoReact, Dictionary, Entertainment, General, Moderation, Owner, Quotes, Rule34, Help)

    var allModules: List<Module> = moduleProviders.map { it.provide() }
    private var loadedModules = allModules.filter { it.isStandard || it.name in config.enabledModules }

    fun resetModules() {
        allModules = moduleProviders.map { it.provide() }
        loadedModules = allModules.filter { it.isStandard || it.name in config.enabledModules }
        Logger.info("Reloaded modules.")
    }

    override fun onGenericEvent(evt: GenericEvent) {
        scope.launch {
            loadedModules.forEach { it.runListeners(evt) }
        }
    }

    override fun onMessageReceived(evt: MessageReceivedEvent) {
        scope.launch {
            if (evt.message.contentRaw.startsWith(config.prefix)) {
                loadedModules.forEach { it.runCommands(evt) }
            }
        }
    }

    class Help : Module() {
        @CommandAnnotation(description = "Sends an embed with a list of commands that can be used by the invoker.")
        fun commands(evt: MessageReceivedEvent) {
            evt.channel.sendEmbed {
                loadedModules.sortedBy { it.name }
                    .mapNotNull { it.getInvokeableCommandList(evt) }
                    .forEach { addField(it) }
            }.complete()
        }

        @CommandAnnotation(description = "Sends an embed with all commands listed.")
        fun allCommands(evt: MessageReceivedEvent) {
            evt.channel.sendEmbed {
                loadedModules.sortedBy { it.name }.forEach { module ->
                    addField(module.commandListField)
                }
            }.complete()
        }

        @CommandAnnotation(description = "Sends an embed with general information on how to use the bot.")
        fun help(evt: MessageReceivedEvent) {
            evt.channel.sendEmbed {
                addField(
                    "Help",
                    """
                        My prefix is `${config.prefix}`.
                        For a list of commands, enter `${config.prefix}commands`.
                        For information on a certain command, enter `${config.prefix}help <command name>`.
                        For a list containing every command, enter `${config.prefix}allcommands`.
                    """.trimIndent(),
                    false
                )
            }.complete()
        }

        @CommandAnnotation(description = "Sends an embed with information about the requested command.")
        fun help(evt: MessageReceivedEvent, commandName: String) {
            val matchingCommands = loadedModules
                .mapNotNull { it.findCommandsByName(commandName) }
                .flatten()
                .filter { it.isNotHidden && it.isInvokeableByAuthor(evt) }
            if (matchingCommands.isNotEmpty()) {
                evt.channel.sendEmbed {
                    matchingCommands.forEachIndexed { index, command ->
                        if (index > 0) addBlankField(false)
                        addField(command.helpField)
                    }
                    setTimestamp(null)
                }.complete()
            } else {
                evt.channel.sendMessage("Could not find any commands matching `$commandName`.").complete()
            }
        }

        companion object : ModuleCompanion {
            override fun provide(): Module = Help()
        }
    }
}
