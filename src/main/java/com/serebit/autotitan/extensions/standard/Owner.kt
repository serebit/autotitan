package com.serebit.autotitan.extensions.standard

import com.serebit.autotitan.api.Access
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import com.serebit.autotitan.config.Configuration
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import kotlin.system.exitProcess

@ExtensionClass
class Owner {
    @CommandFunction(
            description = "Shuts down the bot with an exit code of 0.",
            access = Access.BOT_OWNER
    )
    suspend fun shutdown(evt: MessageReceivedEvent) {
        evt.channel.sendMessage("Shutting down.").complete()
        evt.jda.shutdown()
        Configuration.serialize()
        exitProcess(0)
    }

    @CommandFunction(
            description = "Renames the bot.",
            delimitFinalParameter = false,
            access = Access.BOT_OWNER
    )
    fun setName(evt: MessageReceivedEvent, name: String) {
        evt.jda.selfUser.manager.setName(name).complete()
        evt.channel.sendMessage("Renamed to $name.").complete()
    }

    @CommandFunction(
            description = "Changes the bot's command prefix.",
            access = Access.BOT_OWNER
    )
    fun setPrefix(evt: MessageReceivedEvent, prefix: String) {
        if (prefix.isBlank()) {
            evt.channel.sendMessage("Invalid prefix. Prefix must not be empty or blank.")
        } else {
            Configuration.prefix = if (prefix.length in 1..3) prefix else prefix.substring(0..3)
            Configuration.serialize()
            evt.channel.sendMessage("Set prefix to `${Configuration.prefix}`.").complete()
        }
    }

    @CommandFunction(
            description = "Sends the bot's invite link to the command invoker.",
            access = Access.BOT_OWNER
    )
    fun getInvite(evt: MessageReceivedEvent) {
        evt.author.openPrivateChannel().complete().sendMessage(
                "Invite link: ${evt.jda.asBot().getInviteUrl()}"
        ).complete()
    }

    @CommandFunction(
            description = "Gets the list of servers that the bot is currently in.",
            access = Access.BOT_OWNER
    )
    fun serverList(evt: MessageReceivedEvent) {
        val color = evt.guild?.selfMember?.color
        val embedBuilder = EmbedBuilder()
        embedBuilder.apply {
            setColor(color)
            evt.jda.guilds.forEach {
                addField(
                        it.name + "(${it.id})",
                        "**Text Channels**: ${it.textChannels.size}\n**Members**: ${it.members.size}\n",
                        true
                )
            }
            setTimestamp(OffsetDateTime.now())
        }
        evt.channel.sendMessage(embedBuilder.build()).complete()
    }

    @CommandFunction(
            description = "Leaves the server.",
            access = Access.BOT_OWNER
    )
    fun leaveServer(evt: MessageReceivedEvent) {
        evt.channel.sendMessage("Leaving the server.").complete()
        evt.guild.leave().complete()
    }
}
