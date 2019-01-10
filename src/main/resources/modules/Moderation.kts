import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.module
import com.serebit.autotitan.api.parameters.LongString
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent

class GuildRoleMap : MutableMap<Long, Long> by mutableMapOf() {
    operator fun contains(key: Guild) = contains(key.idLong)

    operator fun get(jda: JDA, key: Guild): Role? = jda.getRoleById(get(key.idLong) ?: -1L)

    operator fun set(key: Guild, value: Role) = put(key.idLong, value.idLong)
}

data class WelcomeMessageData(var channelId: Long, var joinMessage: String? = null, var leaveMessage: String? = null)

val daysOfInactivity = 30
val maximumCleanupCount = 99

module("Moderation") {
    val memberRoleMap: GuildRoleMap = dataManager.readOrDefault("rolemap.json") { GuildRoleMap() }
    val welcomeMessages = dataManager.readOrDefault("welcomemessages.json") {
        mutableMapOf<Long, WelcomeMessageData>()
    }

    command("kick", "Kicks a member.", Access.Guild.All(Permission.KICK_MEMBERS)) { member: Member ->
        guild.controller.kick(member).queue {
            channel.sendMessage("Kicked ${member.effectiveName}.").queue()
        }
    }

    command("ban", "Bans a user.", Access.Guild.All(Permission.BAN_MEMBERS)) { user: User ->
        guild.controller.ban(user, 0).queue {
            channel.sendMessage("Banned ${user.name}.").queue()
        }
    }

    command("unBan", "Un-bans a banned user.", Access.Guild.All(Permission.BAN_MEMBERS)) { user: User ->
        guild.controller.unban(user).queue {
            channel.sendMessage("Unbanned ${user.name}.").complete()
        }
    }

    command(
        "cleanUp",
        "Deletes the last N messages in the channel. N must be in the range of 1 to $maximumCleanupCount.",
        Access.Guild.All(Permission.MESSAGE_MANAGE)
    ) { number: Int ->
        if (number in 1..maximumCleanupCount) {
            textChannel.history.retrievePast(number + 1).queue { messages ->
                textChannel.deleteMessages(messages).queue()
            }
        } else channel.sendMessage("The number has to be in the range of `1..$maximumCleanupCount`.").queue()
    }

    command(
        "setMemberRole",
        "Sets the role given to new members of the server upon joining.",
        Access.Guild.All(Permission.MANAGE_ROLES)
    ) { roleName: LongString ->
        guild.roles
            .findLast { it.name.toLowerCase() == roleName.value.toLowerCase() }
            ?.let { role ->
                memberRoleMap[guild] = role
                dataManager.write("rolemap.json", memberRoleMap)
                channel.sendMessage("Set the member role to `$roleName`.").queue()
            } ?: channel.sendMessage("`$roleName` does not exist.").queue()
    }

    command(
        "getMemberRole",
        "Gets the role given to new members of the server upon joining.",
        Access.Guild.All(Permission.MANAGE_ROLES)
    ) {
        memberRoleMap[jda, guild]?.let { role ->
            channel.sendMessage("The member role for this server is set to `${role.name}`.").queue()
        } ?: channel.sendMessage("The member role is not set up for this server.").queue()
    }

    command(
        "leavemessage",
        "Sets a message to display upon a user leaving the server.",
        Access.Guild.All(Permission.MANAGE_SERVER)
    ) { message: LongString ->
        if (guild.idLong in welcomeMessages) {
            welcomeMessages[guild.idLong]!!.joinMessage = message.value
        } else {
            welcomeMessages[guild.idLong] =
                WelcomeMessageData(guild.systemChannel.idLong, joinMessage = message.value)
        }
        dataManager.write("welcomemessages.json", welcomeMessages)
        channel.sendMessage("Set the join message.").queue()
    }

    command(
        "joinmessage",
        "Sets a message to display upon a user joining the server.",
        Access.Guild.All(Permission.MANAGE_SERVER)
    ) { message: LongString ->
        if (guild.idLong in welcomeMessages) {
            welcomeMessages[guild.idLong]!!.joinMessage = message.value
        } else {
            welcomeMessages[guild.idLong] =
                WelcomeMessageData(guild.systemChannel.idLong, joinMessage = message.value)

        }
        dataManager.write("welcomemessages.json", welcomeMessages)
        channel.sendMessage("Set the join message.").queue()
    }

    command("disablejoinmessage", "Removes the set join message.", Access.Guild.All(Permission.MANAGE_SERVER)) {
        if (welcomeMessages[guild.idLong]?.joinMessage != null) {
            welcomeMessages[guild.idLong]?.joinMessage = null
            channel.sendMessage("Removed the existing join message.").queue()
        } else channel.sendMessage("No join message to remove.").queue()
        dataManager.write("welcomemessages.json", welcomeMessages)
    }

    command("disableleavemessage", "Removes the set leave message.", Access.Guild.All(Permission.MANAGE_SERVER)) {
        if (welcomeMessages[guild.idLong]?.leaveMessage != null) {
            welcomeMessages[guild.idLong]?.leaveMessage = null
            channel.sendMessage("Removed the existing leave message.").queue()
        } else channel.sendMessage("No leave message to remove.").queue()
        dataManager.write("welcomemessages.json", welcomeMessages)
    }

    command(
        "welcomechannel",
        "Sets the channel in which to send leave/join messages.",
        Access.Guild.All(Permission.MANAGE_SERVER)
    ) { channel: TextChannel ->
        if (guild.idLong in welcomeMessages) {
            welcomeMessages[guild.idLong]!!.channelId = channel.idLong
        } else {
            welcomeMessages[guild.idLong] = WelcomeMessageData(channel.idLong)

        }
        dataManager.write("welcomemessages.json", welcomeMessages)
        channel.sendMessage("Set the welcome channel to ${channel.asMention}.").queue()
    }

    listener<GuildMemberJoinEvent> {
        if (guild in memberRoleMap) {
            guild.controller.addRolesToMember(member, memberRoleMap[jda, guild]).queue()
        }
        welcomeMessages[guild.idLong]?.let { data ->
            data.joinMessage?.let {
                val formatted = it.format(user.name, user.asMention)
                guild.getTextChannelById(data.channelId).sendMessage(formatted).queue()
            }
        }
    }

    listener<GuildMemberLeaveEvent> {
        welcomeMessages[guild.idLong]?.let { data ->
            data.leaveMessage?.let {
                val formatted = it.format(user.name, user.asMention)
                guild.getTextChannelById(data.channelId).sendMessage(formatted).queue()
            }
        }
    }
}
