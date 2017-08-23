package com.serebit.autotitan.extensions.extra

import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import com.serebit.autotitan.api.annotations.ListenerFunction
import com.serebit.autotitan.config.Configuration
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@ExtensionClass
class AutoRole {
    @CommandFunction(
            permissions = arrayOf(Permission.MANAGE_ROLES),
            delimitFinalParameter = false
    )
    fun setAutoRole(evt: MessageReceivedEvent, roleName: String) {
        val role = evt.guild.roles.lastOrNull { it.name == roleName }
        if (role != null) {
            evt.channel.sendMessage("Set autorole to `$roleName`.").complete()
            Configuration.autoRoleMap.put(evt.guild, role)
            Configuration.serialize()
        } else {
            evt.channel.sendMessage("`$roleName` does not exist.")
        }
    }

    @ListenerFunction
    fun giveRole(evt: GuildMemberJoinEvent) {
        if (Configuration.autoRoleMap.contains(evt.guild)) {
            evt.guild.controller.addRolesToMember(evt.member, Configuration.autoRoleMap[evt.jda, evt.guild]).complete()
        }
    }
}