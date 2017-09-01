package com.serebit.autotitan.extensions.extra

import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@ExtensionClass
class Nickname {
    @CommandFunction(
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.NICKNAME_MANAGE),
            delimitFinalParameter = false
    )
    fun massNick(evt: MessageReceivedEvent, nickname: String): Unit = evt.run {
        val memberTopRoleIndex = guild.roles.indexOf(member.roles.firstOrNull() ?: run {
            channel.sendMessage("Failed to rename members. You need a role to do that!").complete()
            return
        })
        val selfTopRoleIndex = guild.roles.indexOf(guild.selfMember.roles.firstOrNull() ?: run {
            channel.sendMessage("Failed to rename members. I need a role to do that!").complete()
            return
        })
        channel.sendMessage("Setting nicknames. This may take a while...").complete()
        val validMembers = guild.members.filter {
            var firstRoleIndex = guild.roles.indexOf(it.roles.firstOrNull())
            if (firstRoleIndex == -1) firstRoleIndex = guild.roles.size
            firstRoleIndex > memberTopRoleIndex && firstRoleIndex > selfTopRoleIndex
        }
        validMembers.forEach {
            guild.controller.setNickname(it, nickname).queue()
        }
        channel.sendMessage("Renamed ${validMembers.size} members to $nickname.").complete()
    }

    @CommandFunction(
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.NICKNAME_MANAGE)
    )
    fun massNickReset(evt: MessageReceivedEvent): Unit = evt.run {
        val memberTopRoleIndex = guild.roles.indexOf(member.roles.firstOrNull() ?: run {
            channel.sendMessage("Failed to rename members. You need a role to do that!").complete()
            return
        })
        val selfTopRoleIndex = guild.roles.indexOf(guild.selfMember.roles.firstOrNull() ?: run {
            channel.sendMessage("Failed to rename members. I need a role to do that!").complete()
            return
        })
        channel.sendMessage("Resetting nicknames. This may take a while...").complete()
        val validMembers = guild.members.filter {
            var firstRoleIndex = guild.roles.indexOf(it.roles.firstOrNull())
            if (firstRoleIndex == -1) firstRoleIndex = guild.roles.size
            firstRoleIndex > memberTopRoleIndex && firstRoleIndex > selfTopRoleIndex
        }
        validMembers.forEach {
            guild.controller.setNickname(it, it.user.name).queue()
        }
        channel.sendMessage("Reset the nicknames of ${validMembers.size} members.").complete()
    }
}