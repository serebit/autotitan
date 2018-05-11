package com.serebit.autotitan.modules

import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.parameters.LongString
import com.serebit.autotitan.config
import com.serebit.autotitan.listeners.EventDelegate
import com.serebit.extensions.asMetricUnit
import com.serebit.extensions.asPercentageOf
import com.serebit.extensions.jda.sendEmbed
import com.serebit.extensions.toVerboseTimestamp
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.User
import oshi.SystemInfo
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess


@Suppress("UNUSED")
class Owner : ModuleTemplate(defaultAccess = Access.BotOwner()) {
    private val info by lazy { SystemInfo() }

    init {
        command("shutdown", "Shuts down the bot with an exit code of 0.") { evt ->
            Logger.info("Shutting down...")
            evt.channel.sendMessage("Shutting down.").queue()
            evt.jda.shutdown()
            config.serialize()
            exitProcess(0)
        }

        command("reset", "Resets the modules of the bot, effectively restarting it.") { evt ->
            evt.channel.sendMessage("Resetting...").queue { message ->
                EventDelegate.resetModules()
                message.editMessage("Reset commands and listeners.").queue()
            }
        }

        command("systemInfo", "Gets information about the system that the bot is running on.") { evt ->
            val process = info.operatingSystem.getProcess(info.operatingSystem.processId)
            val processorModel = info.hardware.processor.name.replace("(\\(R\\)|\\(TM\\)|@ .+)".toRegex(), "")
            val processorCores = info.hardware.processor.physicalProcessorCount
            val processorFrequency = info.hardware.processor.vendorFreq
            val totalMemory = info.hardware.memory.total
            val usedMemory = info.hardware.memory.total - info.hardware.memory.available
            val processMemory = process.residentSetSize
            val processUptime = ManagementFactory.getRuntimeMXBean().uptime / millisecondsPerSecond
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
                    Used: `${usedMemory.asMetricUnit("B")} (${usedMemory.asPercentageOf(totalMemory)}%)`
                    Process: `${processMemory.asMetricUnit("B")} (${processMemory.asPercentageOf(totalMemory)}%)`
                    """.trimIndent(),
                    false
                )
                addField(
                    "Uptime",
                    """
                    System: `${info.hardware.processor.systemUptime.toVerboseTimestamp()}`
                    Process: `${processUptime.toVerboseTimestamp()}`
                    """.trimIndent(),
                    false
                )
            }.queue()
        }

        command("setName", "Changes the bot's username.") { evt, name: LongString ->
            if (name.value.length !in 2..usernameMaxLength) {
                evt.channel.sendMessage("Usernames must be between 2 and 32 characters in length.").queue()
            } else {
                evt.jda.selfUser.manager.setName(name.value).queue()
                evt.channel.sendMessage("Renamed to $name.").queue()
            }
        }

        command("setPrefix", "Changes the bot's command prefix.") { evt, prefix: String ->
            if (prefix.isBlank() || prefix.contains("\\s".toRegex())) {
                evt.channel.sendMessage("Invalid prefix. Prefix must not be empty, and may not contain whitespace.")
                return@command
            }
            config.prefix = prefix
            config.serialize()
            evt.jda.presence.game = Game.playing("${prefix}help")
            evt.channel.sendMessage("Set prefix to `${config.prefix}`.").queue()
        }

        command("blackListAdd", "Adds a user to the blacklist.") { evt, user: User ->
            if (user.idLong in config.blackList) {
                evt.channel.sendMessage("${user.name} is already in the blacklist.").queue()
                return@command
            }
            config.blackList.add(user.idLong)
            evt.channel.sendMessage("Added ${user.name} to the blacklist.").queue()
            config.serialize()
        }

        command("blackListRemove", "Removes a user from the blacklist.") { evt, user: User ->
            if (user.idLong in config.blackList) {
                config.blackList.remove(user.idLong)
                config.serialize()
                evt.channel.sendMessage("Removed ${user.name} from the blacklist.").queue()

            } else evt.channel.sendMessage("${user.name} is not in the blacklist.").queue()
        }

        command("blackList", "Sends a list of blacklisted users in an embed.") { evt ->
            if (config.blackList.isNotEmpty()) {
                evt.channel.sendEmbed {
                    addField("Blacklisted Users", config.blackList.joinToString("\n") {
                        evt.jda.getUserById(it).asMention
                    }, true)
                }.queue()
            } else evt.channel.sendMessage("The blacklist is empty.").queue()
        }

        command("getInvite", "Sends the bot's invite link in a private message.") { evt ->
            evt.author.openPrivateChannel().queue {
                it.sendMessage("Invite link: ${evt.jda.asBot().getInviteUrl()}").queue()
            }
        }

        command("serverList", "Sends the list of servers that the bot is in.") { evt ->
            evt.channel.sendEmbed {
                evt.jda.guilds.forEach {
                    addField(
                        it.name,
                        "Owner: ${it.owner.asMention}\nMembers: ${it.members.size}\n",
                        true
                    )
                }
            }.queue()
        }

        command("leaveServer", "Leaves the server in which the command is invoked.") { evt ->
            evt.channel.sendMessage("Leaving the server.").complete()
            evt.guild.leave().queue()
        }
    }

    companion object {
        private const val millisecondsPerSecond = 1000
        private const val usernameMaxLength = 32
    }
}
