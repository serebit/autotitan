package com.serebit.autotitan.modules

import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.config
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.sendEmbed

@Suppress("UNUSED")
class Modules : ModuleTemplate(defaultAccess = Access.BotOwner()) {
    init {
        command("moduleList", "Sends a list of all the modules.") { evt ->
            evt.channel.sendEmbed {
                setTitle("Modules")
                setDescription(EventListener.allModules.joinToString("\n") {
                    it.name + if (it.isOptional) " (Optional)" else ""
                })
            }.queue()
        }

        command("enableModule", "Enables the given optional module.") { evt, moduleName: String ->
            if (EventListener.allModules.filter { it.isOptional }.none { it.name == moduleName }) return@command
            if (moduleName !in config.enabledModules) {
                config.enabledModules.add(moduleName)
                config.serialize()
                evt.channel.sendMessage("Enabled the `$moduleName` module.").queue()
            } else evt.channel.sendMessage("Module `$moduleName` is already enabled.").queue()
        }

        command("disableModule", "Disables the given optional module.") { evt, moduleName: String ->
            if (EventListener.allModules.filter { it.isOptional }.none { it.name == moduleName }) return@command
            if (moduleName in config.enabledModules) {
                config.enabledModules.remove(moduleName)
                config.serialize()
                evt.channel.sendMessage("Disabled the `$moduleName` module.").queue()
            } else evt.channel.sendMessage("Module `$moduleName` is already disabled.").queue()
        }
    }
}
