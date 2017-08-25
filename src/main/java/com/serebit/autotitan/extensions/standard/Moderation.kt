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
    fun kick(evt: MessageReceivedEvent, member: Member) {
        evt.guild.controller.kick(member).complete()
        evt.channel.sendMessage("Kicked ${member.effectiveName}.").complete()
    }

    @CommandFunction(
            description = "Kicks a member and deletes 1 day's worth of their messages.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.BAN_MEMBERS)
    )
    fun softBan(evt: MessageReceivedEvent, member: Member) {
        evt.guild.controller.ban(member, 1).complete()
        evt.guild.controller.unban(member.user).complete()
        evt.channel.sendMessage("Softbanned ${member.effectiveName}.").complete()
    }

    @CommandFunction(
            description = "Bans a user.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.BAN_MEMBERS)
    )
    fun ban(evt: MessageReceivedEvent, user: User) {
        evt.guild.controller.ban(user, 0).complete()
        evt.channel.sendMessage("Banned ${user.name}.")
    }

    @CommandFunction(
            description = "Bans a user from the current server, and deletes 7 days worth of their messages.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.BAN_MEMBERS)
    )
    fun hardBan(evt: MessageReceivedEvent, user: User) {
        evt.guild.controller.ban(user, 7).complete()
        evt.channel.sendMessage("Banned ${user.name}.").queue()
    }

    @CommandFunction(
            description = "Unbans a banned user from the current server.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.BAN_MEMBERS)
    )
    fun unBan(evt: MessageReceivedEvent, user: User) {
        evt.guild.controller.unban(user).queue {
            evt.channel.sendMessage("Unbanned ${user.name}.").queue()
        }
    }

    @CommandFunction(
            description = "Deletes the last N messages in the channel. N must be in the range of 1..99.",
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.MESSAGE_MANAGE)
    )
    fun cleanUp(evt: MessageReceivedEvent, number: Int) {
        if (number in (1..99)) {
            val messages = evt.textChannel.history.retrievePast(number + 1).complete()
            evt.textChannel.deleteMessages(messages).complete()
        } else {
            evt.textChannel.sendMessage("The number has to be in the range of `1..99`.").complete()
        }
    }
}
