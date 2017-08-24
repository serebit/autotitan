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
    fun massNick(evt: MessageReceivedEvent, nickname: String) {
        val memberTopRoleIndex = evt.guild.roles.indexOf(evt.member.roles.firstOrNull() ?: run {
            evt.channel.sendMessage("Failed to rename members. You need a role to do that!").complete()
            return
        })
        val selfTopRoleIndex = evt.guild.roles.indexOf(evt.guild.selfMember.roles.firstOrNull() ?: run {
            evt.channel.sendMessage("Failed to rename members. I need a role to do that!").complete()
            return
        })
        evt.channel.sendMessage("Setting nicknames. This may take a while...").complete()
        val validMembers = evt.guild.members.filter {
            var firstRoleIndex = evt.guild.roles.indexOf(it.roles.firstOrNull())
            if (firstRoleIndex == -1) firstRoleIndex = evt.guild.roles.size
            firstRoleIndex > memberTopRoleIndex && firstRoleIndex > selfTopRoleIndex
        }
        validMembers.forEach {
            evt.guild.controller.setNickname(it, nickname).complete()
        }
        evt.channel.sendMessage("Renamed ${validMembers.size} members to $nickname.").complete()
    }

    @CommandFunction(
            locale = Locale.GUILD,
            permissions = arrayOf(Permission.NICKNAME_MANAGE)
    )
    fun massNickReset(evt: MessageReceivedEvent) {
        val memberTopRoleIndex = evt.guild.roles.indexOf(evt.member.roles.firstOrNull() ?: run {
            evt.channel.sendMessage("Failed to rename members. You need a role to do that!").complete()
            return
        })
        val selfTopRoleIndex = evt.guild.roles.indexOf(evt.guild.selfMember.roles.firstOrNull() ?: run {
            evt.channel.sendMessage("Failed to rename members. I need a role to do that!").complete()
            return
        })
        evt.channel.sendMessage("Resetting nicknames. This may take a while...").complete()
        val validMembers = evt.guild.members.filter {
            var firstRoleIndex = evt.guild.roles.indexOf(it.roles.firstOrNull())
            if (firstRoleIndex == -1) firstRoleIndex = evt.guild.roles.size
            firstRoleIndex > memberTopRoleIndex && firstRoleIndex > selfTopRoleIndex
        }
        validMembers.forEach {
            evt.guild.controller.setNickname(it, it.user.name).complete()
        }
        evt.channel.sendMessage("Reset the nicknames of ${validMembers.size} members.").complete()
    }
}