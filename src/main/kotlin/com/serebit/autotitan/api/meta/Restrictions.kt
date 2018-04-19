package com.serebit.autotitan.api.meta

import com.serebit.extensions.jda.isBotOwner
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

data class Restrictions(
    private val access: Access = Access.All,
    private val permissions: List<Permission> = emptyList(),
    val hidden: Boolean = false
) {
    val description get() = """
        Access: ${access.description}
        Permissions: ${permissions.joinToString()}
    """.trimIndent()

    fun matches(evt: MessageReceivedEvent): Boolean =
         isAccessibleFrom(evt) && evt.member?.hasPermission(permissions.toMutableList()) ?: false

    private fun isAccessibleFrom(evt: MessageReceivedEvent): Boolean = when (access) {
        Access.All -> true
        Access.BotOwner -> evt.author.isBotOwner
        is Access.Private -> isAccessibleFromPrivate(evt, access)
        is Access.Guild -> isAccessibleFromGuild(evt, access)
    }

    private fun isAccessibleFromGuild(evt: MessageReceivedEvent, access: Access.Guild): Boolean =
        evt.guild != null && when (access) {
            Access.Guild.All -> true
            Access.Guild.BotOwner -> evt.author.isBotOwner
            Access.Guild.GuildOwner -> evt.member.isOwner
            Access.Guild.RankAbove -> evt.member.roles[0] > evt.guild.selfMember.roles[0]
            Access.Guild.RankSame -> evt.member.roles[0] == evt.guild.selfMember.roles[0]
            Access.Guild.RankBelow -> evt.member.roles[0] < evt.guild.selfMember.roles[0]
        }

    private fun isAccessibleFromPrivate(evt: MessageReceivedEvent, access: Access.Private): Boolean =
        evt.privateChannel != null && when (access) {
            Access.Private.All -> true
            Access.Private.BotOwner -> evt.author.isBotOwner
        }
}
