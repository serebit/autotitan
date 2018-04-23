package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent

@Suppress("UNUSED", "TooManyFunctions")
class Moderation : Module() {
    private val dataManager = DataManager(this::class)
    private val memberRoleMap: GuildRoleMap = dataManager.read("rolemap.json") ?: GuildRoleMap()

    init {
        command(
            "kick",
            "Kicks a member.",
            Access.Guild.All(Permission.KICK_MEMBERS)
        ) { evt, member: Member ->
            evt.guild.controller.kick(member).complete()
            evt.channel.sendMessage("Kicked ${member.effectiveName}.").complete()
        }

        command(
            "ban",
            "Bans a user.",
            Access.Guild.All(Permission.BAN_MEMBERS)
        ) { evt, user: User ->
            evt.guild.controller.ban(user, 0).complete()
            evt.channel.sendMessage("Banned ${user.name}.")
        }

        command(
            "unBan",
            "Un-bans a banned user from the current server.",
            Access.Guild.All(Permission.BAN_MEMBERS)
        ) { evt, user: User ->
            evt.guild.controller.unban(user).complete()
            evt.channel.sendMessage("Unbanned ${user.name}.").complete()
        }

        command(
            "cleanUp",
            "Deletes the last N messages in the channel. N must be in the range of 1..$maximumCleanupCount.",
            Access.Guild.All(Permission.MESSAGE_MANAGE)
        ) { evt, number: Int ->
            if (number !in 1..maximumCleanupCount) {
                evt.channel.sendMessage("The number has to be in the range of `1..$maximumCleanupCount`.").complete()
                return@command
            }
            val messages = evt.textChannel.history.retrievePast(number + 1).complete()
            evt.textChannel.deleteMessages(messages).complete()
        }

        command(
            "setMemberRole",
            "Sets the role given to new members of the server upon joining.",
            Access.Guild.All(Permission.MANAGE_ROLES),
            false
        ) { evt, roleName: String ->
            val role = evt.guild.roles.findLast { it.name.toLowerCase() == roleName.toLowerCase() } ?: run {
                evt.channel.sendMessage("`$roleName` does not exist.").complete()
                return@command
            }
            evt.channel.sendMessage("Set the member role to `$roleName`.").complete()
            memberRoleMap.put(evt.guild, role)
            dataManager.write("rolemap.json", memberRoleMap)
        }

        command(
            "getMemberRole",
            "Gets the role given to new members of the server upon joining.",
            Access.Guild.All(Permission.MANAGE_ROLES)
        ) { evt ->
            val role = memberRoleMap[evt.jda, evt.guild] ?: run {
                evt.channel.sendMessage("The member role is not set up for this server.").complete()
                return@command
            }
            evt.channel.sendMessage("The member role for this server is set to `${role.name}`.").complete()
        }

        command(
            "smartPrune",
            "Prunes members from the server, including those with the default member role.",
            Access.Guild.All(Permission.MANAGE_SERVER)
        ) { evt ->
            memberRoleMap[evt.jda, evt.guild]?.let { memberRole ->
                val membersWithBaseRole = evt.guild.getMembersWithRoles(memberRole)
                membersWithBaseRole.forEach { evt.guild.controller.removeSingleRoleFromMember(it, memberRole).queue() }
                val prunableMemberCount = evt.guild.getPrunableMemberCount(daysOfInactivity).complete()
                evt.guild.controller.prune(daysOfInactivity).complete()
                val membersWithoutBaseRole = evt.guild.members.filter { it.roles.isEmpty() }
                membersWithoutBaseRole.forEach { evt.guild.controller.addSingleRoleToMember(it, memberRole).queue() }
                evt.channel.sendMessage("Pruned $prunableMemberCount members.").complete()
            } ?: run {
                val prunableMemberCount = evt.guild.getPrunableMemberCount(daysOfInactivity).complete()
                evt.guild.controller.prune(daysOfInactivity).complete()
                evt.channel.sendMessage("Pruned $prunableMemberCount members.").complete()
            }
        }

        listener { evt: GuildMemberJoinEvent ->
            if (memberRoleMap.contains(evt.guild)) {
                evt.guild.controller.addRolesToMember(evt.member, memberRoleMap[evt.jda, evt.guild]).complete()
            }
        }
    }

    private class GuildRoleMap {
        private val map = mutableMapOf<Long, Long>()

        operator fun contains(key: Guild) = map.contains(key.idLong)

        operator fun get(jda: JDA, key: Guild): Role? = jda.getRoleById(map[key.idLong] ?: -1L)

        fun put(key: Guild, value: Role) = map.put(key.idLong, value.idLong)
    }

    companion object {
        private const val daysOfInactivity = 30
        private const val maximumCleanupCount = 99
    }
}
