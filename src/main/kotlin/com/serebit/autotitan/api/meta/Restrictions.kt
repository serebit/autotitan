package com.serebit.autotitan.api.meta

import com.serebit.extensions.jda.isBotOwner
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

data class Restrictions(
    val access: Access = Access.All,
    val permissions: List<Permission> = emptyList(),
    val hidden: Boolean = false
) {
    fun isAccessibleFrom(evt: MessageReceivedEvent): Boolean = when (access) {
        Access.All -> true
        Access.BotOwner -> evt.author.isBotOwner
        is Access.Private -> evt.privateChannel != null && when (access) {
            Access.Private.All -> true
            Access.Private.BotOwner -> evt.author.isBotOwner
        }
        is Access.Guild -> evt.guild != null && when (access) {
            Access.Guild.All -> true
            Access.Guild.BotOwner -> evt.author.isBotOwner
            Access.Guild.GuildOwner -> evt.member.isOwner
            Access.Guild.RankAbove -> evt.member.roles[0] > evt.guild.selfMember.roles[0]
            Access.Guild.RankSame -> evt.member.roles[0] == evt.guild.selfMember.roles[0]
            Access.Guild.RankBelow -> evt.member.roles[0] < evt.guild.selfMember.roles[0]
        }
    }
}
