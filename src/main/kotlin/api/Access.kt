package com.serebit.autotitan.api

import com.serebit.autotitan.extensions.isBotOwner
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

sealed class Access(val description: String, val hidden: Boolean) {
    abstract fun matches(evt: MessageReceivedEvent): Boolean

    class All(hidden: Boolean = false) : Access("Anyone", hidden) {
        override fun matches(evt: MessageReceivedEvent): Boolean = true
    }

    class BotOwner(hidden: Boolean = false) : Access("Bot owner only", hidden) {
        override fun matches(evt: MessageReceivedEvent): Boolean = evt.author.isBotOwner
    }

    sealed class Private(description: String, hidden: Boolean) : Access(description, hidden) {
        override fun matches(evt: MessageReceivedEvent): Boolean = evt.privateChannel != null

        class All(hidden: Boolean = false) : Private("In private messages only", hidden)

        class BotOwner(hidden: Boolean = false) : Private("Bot owner in private messages only", hidden) {
            override fun matches(evt: MessageReceivedEvent): Boolean = super.matches(evt) && evt.author.isBotOwner
        }
    }

    sealed class Guild(
        description: String,
        hidden: Boolean,
        private val permissions: Set<Permission>
    ) : Access("$description\nPermissions: ${permissions.joinToString()}", hidden) {
        override fun matches(evt: MessageReceivedEvent): Boolean = evt.member?.hasPermission(permissions) ?: false

        class All(
            vararg permissions: Permission,
            hidden: Boolean = false
        ) : Guild("Servers only", hidden, permissions.toSet())

        class BotOwner(
            hidden: Boolean = false,
            vararg permissions: Permission
        ) : Guild("Bot owner in servers only", hidden, permissions.toSet()) {
            override fun matches(evt: MessageReceivedEvent): Boolean = super.matches(evt) && evt.author.isBotOwner
        }

        class GuildOwner(
            vararg permissions: Permission,
            hidden: Boolean = false
        ) : Guild("Server owner only", hidden, permissions.toSet()) {
            override fun matches(evt: MessageReceivedEvent): Boolean = super.matches(evt) && evt.member.isOwner
        }

        class RankAbove(
            vararg permissions: Permission,
            hidden: Boolean = false
        ) : Guild("Anyone with their top role above the bot's top role", hidden, permissions.toSet()) {
            override fun matches(evt: MessageReceivedEvent): Boolean =
                super.matches(evt) && evt.member.rolePosition > evt.guild.selfMember.rolePosition
        }

        class RankSame(
            vararg permissions: Permission,
            hidden: Boolean = false
        ) : Guild("Anyone with the same top role as the bot's top role", hidden, permissions.toSet()) {
            override fun matches(evt: MessageReceivedEvent): Boolean =
                super.matches(evt) && evt.member.rolePosition == evt.guild.selfMember.rolePosition
        }

        class RankBelow(
            vararg permissions: Permission,
            hidden: Boolean = false
        ) : Guild("Anyone with their top role below the bot's top role", hidden, permissions.toSet()) {
            override fun matches(evt: MessageReceivedEvent): Boolean =
                super.matches(evt) && evt.member.rolePosition < evt.guild.selfMember.rolePosition
        }
    }

    protected val Member.rolePosition get() = roles.getOrNull(0)?.position ?: -1
}
