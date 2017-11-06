package com.serebit.autotitan.modules.standard

import com.serebit.autotitan.api.Access
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import com.serebit.autotitan.config
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import kotlin.system.exitProcess

@ExtensionClass
class Owner {
    @CommandFunction(
            description = "Shuts down the bot with an exit code of 0.",
            access = Access.BOT_OWNER
    )
    suspend fun shutdown(evt: MessageReceivedEvent): Unit = evt.run {
        channel.sendMessage("Shutting down.").complete()
        jda.shutdown()
        config.serialize()
        exitProcess(0)
    }

    @CommandFunction(
            description = "Renames the bot.",
            delimitFinalParameter = false,
            access = Access.BOT_OWNER
    )
    fun setName(evt: MessageReceivedEvent, name: String): Unit = evt.run {
        jda.selfUser.manager.setName(name).complete()
        channel.sendMessage("Renamed to $name.").complete()
    }

    @CommandFunction(
            description = "Changes the bot's command prefix.",
            access = Access.BOT_OWNER
    )
    fun setPrefix(evt: MessageReceivedEvent, prefix: String): Unit = evt.run {
        if (prefix.isBlank()) {
            channel.sendMessage("Invalid prefix. Prefix must not be empty or blank.")
        } else {
            config.prefix = if (prefix.length in 1..3) prefix else prefix.substring(0..3)
            config.serialize()
            channel.sendMessage("Set prefix to `${config.prefix}`.").complete()
        }
    }

    @CommandFunction(
            description = "Adds a user to the blacklist.",
            access = Access.BOT_OWNER
    )
    fun blackListAdd(evt: MessageReceivedEvent, user: User): Unit = evt.run {
        if (user.idLong in config.blackList) {
            channel.sendMessage("${user.name} is already in the blacklist.").complete()
            return
        }
        config.blackList.add(user.idLong)
        channel.sendMessage("Added ${user.name} to the blacklist.").complete()
        config.serialize()
    }

    @CommandFunction(
            description = "Removes a user from the blacklist.",
            access = Access.BOT_OWNER
    )
    fun blackListRemove(evt: MessageReceivedEvent, user: User): Unit = evt.run {
        if (user.idLong !in config.blackList) {
            channel.sendMessage("${user.name} is not in the blacklist.").complete()
            return
        }
        config.blackList.remove(user.idLong)
        channel.sendMessage("Removed ${user.name} from the blacklist.").complete()
        config.serialize()
    }

    @CommandFunction(
            description = "Sends the blacklist.",
            access = Access.BOT_OWNER
    )
    fun blackList(evt: MessageReceivedEvent) {
        evt.run {
            if (config.blackList.isNotEmpty()) {
                channel.sendEmbed {
                    addField("Blacklisted Users", config.blackList.joinToString("\n") {
                        jda.getUserById(it).asMention
                    }, true)
                }.complete()
            } else {
                channel.sendMessage("The blacklist is empty.").complete()
            }

        }
    }

    @CommandFunction(
            description = "Sends the bot's invite link to the command invoker.",
            access = Access.BOT_OWNER
    )
    fun getInvite(evt: MessageReceivedEvent): Unit = evt.run {
        author.openPrivateChannel().complete().sendMessage(
                "Invite link: ${jda.asBot().getInviteUrl()}"
        ).complete()
    }

    @CommandFunction(
            description = "Gets the list of servers that the bot is currently in.",
            access = Access.BOT_OWNER
    )
    fun serverList(evt: MessageReceivedEvent): Unit = evt.run {
        val color = guild?.selfMember?.color
        val embedBuilder = EmbedBuilder()
        embedBuilder.apply {
            setColor(color)
            jda.guilds.forEach {
                addField(
                        it.name + "(${it.id})",
                        "**Text Channels**: ${it.textChannels.size}\n**Members**: ${it.members.size}\n",
                        true
                )
            }
            setTimestamp(OffsetDateTime.now())
        }
        channel.sendMessage(embedBuilder.build()).complete()
    }

    @CommandFunction(
            description = "Leaves the server.",
            access = Access.BOT_OWNER
    )
    fun leaveServer(evt: MessageReceivedEvent): Unit = evt.run {
        channel.sendMessage("Leaving the server.").complete()
        guild.leave().complete()
    }
}
