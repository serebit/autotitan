package com.serebit.autotitan.listeners

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.config
import com.serebit.extensions.jda.sendEmbed
import com.serebit.loggerkt.Logger
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.reflections.Reflections
import kotlin.reflect.full.createInstance

internal object EventDelegate : ListenerAdapter() {
    var allModules: List<Module> = classpathModules
        private set
    private val classpathModules
        get() = Reflections("com.serebit.autotitan.modules")
            .getSubTypesOf(ModuleTemplate::class.java)
            .mapNotNull { it.kotlin.createInstance().build() } + Help().build()
    private val loadedModules get() = allModules.filter { it.isStandard || it.name in config.enabledModules }

    fun resetModules() {
        allModules = classpathModules
        Logger.info("Reloaded modules from classpath.")
    }

    override fun onGenericEvent(evt: Event) {
        launch {
            loadedModules.forEach { it.invoke(evt) }
        }
    }

    class Help : ModuleTemplate() {
        init {
            command("commands", "Sends an embed with a list of commands that can be used by the invoker.") { evt ->
                evt.channel.sendEmbed {
                    loadedModules.sortedBy { it.name }
                        .mapNotNull { it.getInvokeableCommandField(evt) }
                        .forEach { addField(it) }
                }.queue()
            }

            command("allCommands", description = "Sends an embed with all commands listed.") {
                it.channel.sendEmbed {
                    loadedModules.sortedBy { it.name }.forEach { module ->
                        addField(module.commandListField)
                    }
                }.queue()
            }

            command("help", description = "Sends an embed with general information on how to use the bot.") {
                it.channel.sendEmbed {
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
                }.queue()
            }

            command(
                "help",
                "Sends an embed with information about the requested command."
            ) { evt, commandName: String ->
                val matchingCommands = loadedModules
                    .map { it.findCommandsByName(commandName) }
                    .filter { it.isNotEmpty() }
                    .flatten()
                if (matchingCommands.isNotEmpty()) {
                    evt.channel.sendEmbed {
                        matchingCommands.forEachIndexed { index, command ->
                            if (index > 0) addBlankField(false)
                            addField(command.helpField)
                        }
                    }.queue()
                } else evt.channel.sendMessage("Could not find any commands matching `$commandName`.").queue()
            }
        }
    }
}
