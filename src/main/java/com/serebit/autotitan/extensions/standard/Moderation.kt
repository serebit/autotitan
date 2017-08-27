package com.serebit.autotitan.extensions.standard

import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@ExtensionClass
class Moderation {
    @CommandFunction(
            description = "Kicks a member.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.KICK_MEMBERS)
    )
    fun kick(evt: MessageReceivedEvent, member: Member): Unit = evt.run {
        guild.controller.kick(member).complete()
        channel.sendMessage("Kicked ${member.effectiveName}.").complete()
    }

    @CommandFunction(
            description = "Bans a user.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.BAN_MEMBERS)
    )
    fun ban(evt: MessageReceivedEvent, user: User): Unit = evt.run {
        guild.controller.ban(user, 0).complete()
        channel.sendMessage("Banned ${user.name}.")
    }

    @CommandFunction(
            description = "Unbans a banned user from the current server.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.BAN_MEMBERS)
    )
    fun unBan(evt: MessageReceivedEvent, user: User): Unit = evt.run {
        guild.controller.unban(user).complete()
        channel.sendMessage("Unbanned ${user.name}.").complete()
    }

    @CommandFunction(
            description = "Deletes the last N messages in the channel. N must be in the range of 1..99.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.MESSAGE_MANAGE)
    )
    fun cleanUp(evt: MessageReceivedEvent, number: Int): Unit = evt.run {
        if (number in 1..99) {
            val messages = textChannel.history.retrievePast(number + 1).complete()
            textChannel.deleteMessages(messages).complete()
        } else {
            textChannel.sendMessage("The number has to be in the range of `1..99`.").complete()
        }
    }
}
