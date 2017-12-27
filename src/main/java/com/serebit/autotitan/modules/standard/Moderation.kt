package com.serebit.autotitan.modules.standard

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.api.meta.annotations.Listener
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Moderation : Module() {
    private val dataManager = DataManager(this::class.java)
    private val map: GuildRoleMap = dataManager.read("rolemap.json") ?: GuildRoleMap()

    init {
        dataManager.write("rolemap.json", map)
    }

    @Command(
            description = "Kicks a member.",
            locale = Locale.GUILD,
            memberPermissions = [Permission.KICK_MEMBERS]
    )
    fun kick(evt: MessageReceivedEvent, member: Member) {
        evt.run {
            guild.controller.kick(member).complete()
            channel.sendMessage("Kicked ${member.effectiveName}.").complete()
        }
    }

    @Command(
            description = "Bans a user.",
            locale = Locale.GUILD,
            memberPermissions = [Permission.BAN_MEMBERS]
    )
    fun ban(evt: MessageReceivedEvent, user: User) {
        evt.run {
            guild.controller.ban(user, 0).complete()
            channel.sendMessage("Banned ${user.name}.")
        }
    }

    @Command(
            description = "Unbans a banned user from the current server.",
            locale = Locale.GUILD,
            memberPermissions = [Permission.BAN_MEMBERS]
    )
    fun unBan(evt: MessageReceivedEvent, user: User) {
        evt.run {
            guild.controller.unban(user).complete()
            channel.sendMessage("Unbanned ${user.name}.").complete()
        }
    }

    @Command(
            description = "Deletes the last N messages in the channel. N must be in the range of 1..99.",
            locale = Locale.GUILD,
            memberPermissions = [Permission.MESSAGE_MANAGE]
    )
    fun cleanUp(evt: MessageReceivedEvent, number: Int) {
        evt.run {
            if (number !in 1..99) {
                channel.sendMessage("The number has to be in the range of `1..99`.").complete()
                return
            } else {
                val messages = textChannel.history.retrievePast(number + 1).complete()
                textChannel.deleteMessages(messages).complete()
            }
        }
    }

    @Command(
            memberPermissions = [Permission.MANAGE_ROLES],
            splitLastParameter = false
    )
    fun setAutoRole(evt: MessageReceivedEvent, roleName: String) {
        evt.run {
            val role = guild.roles.lastOrNull { it.name.toLowerCase() == roleName.toLowerCase() }
            if (role != null) {
                channel.sendMessage("Set autorole to `$roleName`.").complete()
                map.put(guild, role)
                dataManager.write("rolemap.json", map)
            } else {
                channel.sendMessage("`$roleName` does not exist.").complete()
            }
        }
    }

    @Command(
            memberPermissions = [Permission.MANAGE_ROLES]
    )
    fun getAutoRole(evt: MessageReceivedEvent) {
        evt.run {
            val role = map[jda, guild] ?: run {
                channel.sendMessage("Autorole is not set up for this guild.").complete()
                return
            }
            channel.sendMessage("The autorole for this server is set to `${role.name}`.").complete()
        }
    }

    @Command(
            memberPermissions = [Permission.MANAGE_SERVER]
    )
    fun smartPrune(evt: MessageReceivedEvent) {
        evt.run {
            val baseRole = map[jda, guild]
            if (baseRole != null) {
                val membersWithBaseRole = guild.getMembersWithRoles(baseRole)
                membersWithBaseRole.forEach { guild.controller.removeSingleRoleFromMember(it, baseRole).queue() }
                val prunableMemberCount = guild.getPrunableMemberCount(30).complete()
                guild.controller.prune(30).complete()
                val membersWithoutBaseRole = guild.members.filter { it.roles.isEmpty() }
                membersWithoutBaseRole.forEach { guild.controller.addSingleRoleToMember(it, baseRole).queue() }
                channel.sendMessage("Pruned $prunableMemberCount members.").complete()
            } else {
                val prunableMemberCount = guild.getPrunableMemberCount(30).complete()
                guild.controller.prune(30).complete()
                channel.sendMessage("Pruned $prunableMemberCount members.").complete()
            }
        }
    }

    @Listener
    fun giveRole(evt: GuildMemberJoinEvent) {
        evt.run {
            if (map.contains(guild)) {
                guild.controller.addRolesToMember(member, map[jda, guild]).complete()
            }
        }
    }

    private class GuildRoleMap {
        private val map = mutableMapOf<Long, Long>()

        operator fun contains(key: Guild) = map.contains(key.idLong)

        operator fun get(jda: JDA, key: Guild): Role? {
            val value = map[key.idLong]
            return if (value != null) jda.getRoleById(value) else null
        }

        fun put(key: Guild, value: Role) = map.put(key.idLong, value.idLong)
    }
}