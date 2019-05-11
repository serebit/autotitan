package com.serebit.autotitan.listeners

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.Logger
import com.serebit.autotitan.api.Module
import com.serebit.autotitan.config
import com.serebit.autotitan.extensions.jda.sendEmbed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import kotlin.reflect.full.createInstance
import com.serebit.autotitan.api.annotations.Command as CommandAnnotation

internal object EventListener : ListenerAdapter(), CoroutineScope {
    override val coroutineContext = Dispatchers.Default
    var allModules: List<Module> = classpathModules
        private set
    private val classpathModules
        get() = ClassPath
            .from(Thread.currentThread().contextClassLoader)
            .getTopLevelClassesRecursive("com.serebit.autotitan.modules")
            .mapNotNull { it.load().kotlin.createInstance() as Module } + Help()
    private val loadedModules get() = allModules.filter { it.isStandard || it.name in config.enabledModules }

    fun resetModules() {
        allModules = classpathModules
        Logger.info("Reloaded modules from classpath.")
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
    }
}
