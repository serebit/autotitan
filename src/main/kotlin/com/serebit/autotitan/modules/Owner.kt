package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.config
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.asMetricUnit
import com.serebit.extensions.asPercentageOf
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.toVerboseTimestamp
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import oshi.SystemInfo
import kotlin.system.exitProcess

@Suppress("UNUSED", "TooManyFunctions")
class Owner : Module() {
    @Command(
        description = "Shuts down the bot with an exit code of 0.",
        access = Access.BOT_OWNER
    )
    fun shutdown(evt: MessageReceivedEvent) {
        Logger.info("Shutting down...")
        evt.channel.sendMessage("Shutting down.").complete()
        evt.jda.shutdown()
        config.serialize()
        exitProcess(0)
    }

    @Command(
        description = "Resets the command and listener classes of the bot, effectively restarting it.",
        access = Access.BOT_OWNER
    )
    fun reset(evt: MessageReceivedEvent) {
        val message = evt.channel.sendMessage("Resetting...").complete()
        EventListener.resetModules()
        message.editMessage("Reset commands and listeners.").complete()
    }

    @Command(
        description = "Gets information about the system that the bot is running on.",
        access = Access.BOT_OWNER
    )
    fun systemInfo(evt: MessageReceivedEvent) {
        val info = SystemInfo()
        val process = info.operatingSystem.getProcess(info.operatingSystem.processId)
        val processorModel = info.hardware.processor.name.replace("(\\(R\\)|\\(TM\\)|@ .+)".toRegex(), "")
        val processorCores = info.hardware.processor.physicalProcessorCount
        val processorFrequency = info.hardware.processor.vendorFreq
        val totalMemory = info.hardware.memory.total
        val usedMemory = info.hardware.memory.total - info.hardware.memory.available
        val usedMemoryPercentage = usedMemory.asPercentageOf(totalMemory)
        val processMemory = process.residentSetSize
        val processMemoryPercentage = processMemory.asPercentageOf(totalMemory)
        val systemUptime = info.hardware.processor.systemUptime
        val processUptime = process.upTime / millisecondsPerSecond
        evt.channel.sendEmbed {
            addField(
                "Processor",
                """
                    Model: `$processorModel`
                    Cores: `$processorCores`
                    Frequency: `${processorFrequency.asMetricUnit("Hz")}`
                """.trimIndent(),
                false
            )
            addField(
                "Memory",
                """
                    Total: `${totalMemory.asMetricUnit("B")}`
                    Used: `${usedMemory.asMetricUnit("B")} ($usedMemoryPercentage%)`
                    Process: `${processMemory.asMetricUnit("B")} ($processMemoryPercentage%)`
                """.trimIndent(),
                false
            )
            addField(
                "Uptime",
                """
                    System: `${systemUptime.toVerboseTimestamp()}`
                    Process: `${processUptime.toVerboseTimestamp()}`
                """.trimIndent(),
                false
            )
        }.complete()
    }

    @Command(
        description = "Renames the bot.",
        splitLastParameter = false,
        access = Access.BOT_OWNER
    )
    fun setName(evt: MessageReceivedEvent, name: String) {
        evt.jda.selfUser.manager.setName(name).complete()
        evt.channel.sendMessage("Renamed to $name.").complete()
    }

    @Command(
        description = "Changes the bot's command prefix.",
        access = Access.BOT_OWNER
    )
    fun setPrefix(evt: MessageReceivedEvent, prefix: String) {
        if (prefix.isBlank() || prefix.contains("\\s".toRegex())) {
            evt.channel.sendMessage("Invalid prefix. Prefix must not be empty, and may not contain whitespace.")
            return
        }
        config.prefix = prefix
        config.serialize()
        evt.jda.presence.game = Game.playing("${prefix}help")
        evt.channel.sendMessage("Set prefix to `${config.prefix}`.").complete()
    }

    @Command(
        description = "Adds a user to the blacklist.",
        access = Access.BOT_OWNER
    )
    fun blackListAdd(evt: MessageReceivedEvent, user: User) {
        if (user.idLong in config.blackList) {
            evt.channel.sendMessage("${user.name} is already in the blacklist.").complete()
            return
        }
        config.blackList.add(user.idLong)
        evt.channel.sendMessage("Added ${user.name} to the blacklist.").complete()
        config.serialize()
    }

    @Command(
        description = "Removes a user from the blacklist.",
        access = Access.BOT_OWNER
    )
    fun blackListRemove(evt: MessageReceivedEvent, user: User) {
        if (user.idLong !in config.blackList) {
            evt.channel.sendMessage("${user.name} is not in the blacklist.").complete()
            return
        }
        config.blackList.remove(user.idLong)
        evt.channel.sendMessage("Removed ${user.name} from the blacklist.").complete()
        config.serialize()
    }

    @Command(
        description = "Sends the blacklist.",
        access = Access.BOT_OWNER
    )
    fun blackList(evt: MessageReceivedEvent) {
        if (config.blackList.isNotEmpty()) {
            evt.channel.sendEmbed {
                addField("Blacklisted Users", config.blackList.joinToString("\n") {
                    evt.jda.getUserById(it).asMention
                }, true)
            }.complete()
        } else {
            evt.channel.sendMessage("The blacklist is empty.").complete()
        }
    }

    @Command(
        description = "Sends the bot's invite link to the command invoker.",
        access = Access.BOT_OWNER
    )
    fun getInvite(evt: MessageReceivedEvent) {
        evt.author.openPrivateChannel().complete().sendMessage(
            "Invite link: ${evt.jda.asBot().getInviteUrl()}"
        ).complete()
    }

    @Command(
        description = "Gets the list of servers that the bot is currently in.",
        access = Access.BOT_OWNER
    )
    fun serverList(evt: MessageReceivedEvent) {
        evt.channel.sendEmbed {
            evt.jda.guilds.forEach {
                addField(
                    it.name + "(${it.id})",
                    "**Text Channels**: ${it.textChannels.size}\n**Members**: ${it.members.size}\n",
                    true
                )
            }
        }.complete()
    }

    @Command(
        description = "Leaves the server.",
        access = Access.BOT_OWNER
    )
    fun leaveServer(evt: MessageReceivedEvent) {
        evt.channel.sendMessage("Leaving the server.").complete()
        evt.guild.leave().complete()
    }

    @Command(description = "Sends a list of all the modules.", access = Access.BOT_OWNER)
    fun moduleList(evt: MessageReceivedEvent) {
        evt.channel.sendEmbed {
            setTitle("Modules")
            setDescription(EventListener.allModules.joinToString("\n") {
                it.name + if (it.isOptional) " (Optional)" else ""
            })
        }.complete()
    }

    @Command(description = "Enables the given optional module.", access = Access.BOT_OWNER)
    fun enableModule(evt: MessageReceivedEvent, moduleName: String) {
        if (EventListener.allModules.filter { it.isOptional }.none { it.name == moduleName }) return
        if (moduleName in config.enabledModules) {
            evt.channel.sendMessage("Module `$moduleName` is already enabled.").complete()
            return
        }
        config.enabledModules.add(moduleName)
        config.serialize()
        evt.channel.sendMessage("Enabled the `$moduleName` module.").complete()
    }

    @Command(description = "Disables the given optional module.", access = Access.BOT_OWNER)
    fun disableModule(evt: MessageReceivedEvent, moduleName: String) {
        if (EventListener.allModules.filter { it.isOptional }.none { it.name == moduleName }) return
        if (moduleName !in config.enabledModules) {
            evt.channel.sendMessage("Module `$moduleName` is already disabled.").complete()
            return
        }
        config.enabledModules.remove(moduleName)
        config.serialize()
        evt.channel.sendMessage("Disabled the `$moduleName` module.").complete()
    }

    companion object {
        private const val millisecondsPerSecond = 1000
    }
}
