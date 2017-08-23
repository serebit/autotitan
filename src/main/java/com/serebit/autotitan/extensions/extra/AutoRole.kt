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
            permissions = arrayOf(Permission.MANAGE_ROLES)
    )
    fun setAutoRole(evt: MessageReceivedEvent, roleName: String) {
        Configuration.autoRoleMap.put(evt.guild, evt.guild.roles.lastOrNull { it.name == roleName })
        Configuration.serialize()
    }

    @ListenerFunction
    fun giveRole(evt: GuildMemberJoinEvent) {
        if (Configuration.autoRoleMap.contains(evt.guild)) {
            evt.guild.controller.addRolesToMember(evt.member, Configuration.autoRoleMap[evt.jda, evt.guild]).complete()
        }
    }
}