package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.config
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.sendEmbed
import com.serebit.loggerkt.Logger
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.system.exitProcess

@Suppress("UNUSED")
class Owner : Module() {
    @Command(
        description = "Shuts down the bot with an exit code of 0.",
        access = Access.BOT_OWNER
    )
    fun shutdown(evt: MessageReceivedEvent) {
        Logger.info("Shutting down...")
        evt.run {
            channel.sendMessage("Shutting down.").complete()
            jda.shutdown()
            config.serialize()
            exitProcess(0)
        }
    }

    @Command(
        description = "Resets the command and listener classes of the bot, effectively restarting it.",
        access = Access.BOT_OWNER
    )
    fun reset(evt: MessageReceivedEvent) {
        evt.run {
            val message = channel.sendMessage("Resetting...").complete()
            EventListener.resetModules()
            message.editMessage("Reset commands and listeners.").complete()
        }
    }

    @Command(
        description = "Renames the bot.",
        splitLastParameter = false,
        access = Access.BOT_OWNER
    )
    fun setName(evt: MessageReceivedEvent, name: String) {
        evt.run {
            jda.selfUser.manager.setName(name).complete()
            channel.sendMessage("Renamed to $name.").complete()
        }
    }

    @Command(
        description = "Changes the bot's command prefix.",
        access = Access.BOT_OWNER
    )
    fun setPrefix(evt: MessageReceivedEvent, prefix: String) {
        evt.run {
            if (prefix.isBlank() || prefix.contains("\\s".toRegex())) {
                channel.sendMessage("Invalid prefix. Prefix must not be empty, and may not contain whitespace.")
                return
            }
            config.prefix = prefix
            config.serialize()
            channel.sendMessage("Set prefix to `${config.prefix}`.").complete()
        }
    }

    @Command(
        description = "Adds a user to the blacklist.",
        access = Access.BOT_OWNER
    )
    fun blackListAdd(evt: MessageReceivedEvent, user: User) {
        evt.run {
            if (user.idLong in config.blackList) {
                channel.sendMessage("${user.name} is already in the blacklist.").complete()
                return
            }
            config.blackList.add(user.idLong)
            channel.sendMessage("Added ${user.name} to the blacklist.").complete()
            config.serialize()
        }
    }

    @Command(
        description = "Removes a user from the blacklist.",
        access = Access.BOT_OWNER
    )
    fun blackListRemove(evt: MessageReceivedEvent, user: User) {
        evt.run {
            if (user.idLong !in config.blackList) {
                channel.sendMessage("${user.name} is not in the blacklist.").complete()
                return
            }
            config.blackList.remove(user.idLong)
            channel.sendMessage("Removed ${user.name} from the blacklist.").complete()
            config.serialize()
        }
    }

    @Command(
        description = "Sends the blacklist.",
        access = Access.BOT_OWNER
    )
    fun blackList(evt: MessageReceivedEvent) {
        evt.run {
            if (config.blackList.isEmpty()) {
                channel.sendMessage("The blacklist is empty.").complete()
                return
            }
            channel.sendEmbed {
                addField("Blacklisted Users", config.blackList.joinToString("\n") {
                    jda.getUserById(it).asMention
                }, true)
            }.complete()
        }
    }

    @Command(
        description = "Sends the bot's invite link to the command invoker.",
        access = Access.BOT_OWNER
    )
    fun getInvite(evt: MessageReceivedEvent) {
        evt.run {
            author.openPrivateChannel().complete().sendMessage(
                "Invite link: ${jda.asBot().getInviteUrl()}"
            ).complete()
        }
    }

    @Command(
        description = "Gets the list of servers that the bot is currently in.",
        access = Access.BOT_OWNER
    )
    fun serverList(evt: MessageReceivedEvent) {
        evt.run {
            channel.sendEmbed {
                jda.guilds.forEach {
                    addField(
                        it.name + "(${it.id})",
                        "**Text Channels**: ${it.textChannels.size}\n**Members**: ${it.members.size}\n",
                        true
                    )
                }
            }.complete()
        }
    }

    @Command(
        description = "Leaves the server.",
        access = Access.BOT_OWNER
    )
    fun leaveServer(evt: MessageReceivedEvent) {
        evt.run {
            channel.sendMessage("Leaving the server.").complete()
            guild.leave().complete()
        }
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
}
