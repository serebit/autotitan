package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.format.DateTimeFormatter

@Suppress("UNUSED")
class General : Module() {
    private val dateFormat = DateTimeFormatter.ofPattern("d MMM, yyyy")

    @Command(description = "Pings the bot.")
    fun ping(evt: MessageReceivedEvent) {
        evt.channel.sendMessage("Pong. The last ping was ${evt.jda.ping}ms.").complete()
    }

    @Command(description = "Gets information about the server.", locale = Locale.GUILD)
    fun serverInfo(evt: MessageReceivedEvent) {
        val onlineMemberCount = evt.guild.members.count { it.onlineStatus != OnlineStatus.OFFLINE }
        val hoistedRoles = evt.guild.roles
            .filter { it.name != "@everyone" && it.isHoisted }
            .joinToString(", ") { it.name }

        evt.channel.sendEmbed {
            setTitle(evt.guild.name, null)
            setDescription("Created on ${evt.guild.creationTime.format(dateFormat)}")
            setThumbnail(evt.guild.iconUrl)
            addField("Owner", evt.guild.owner.asMention, true)
            addField("Region", evt.guild.region.toString(), true)
            addField("Online Members", onlineMemberCount.toString(), true)
            addField("Total Members", evt.guild.members.size.toString(), true)
            addField("Bots", evt.guild.members.count { it.user.isBot }.toString(), true)
            addField("Text Channels", evt.guild.textChannels.size.toString(), true)
            addField("Voice Channels", evt.guild.voiceChannels.size.toString(), true)
            addField("Hoisted Roles", hoistedRoles, true)
            if (evt.guild.selfMember.hasPermission(Permission.MANAGE_SERVER)) {
                val permanentInvites = evt.guild.invites.complete().filter { !it.isTemporary }
                if (permanentInvites.isNotEmpty()) addField(
                    "Invite Link",
                    permanentInvites.first().url,
                    false
                )
            }
            setFooter("Server ID: ${evt.guild.id}", null)
        }.complete()
    }

    @Command(description = "Gets information about the invoker.", locale = Locale.GUILD)
    fun selfInfo(evt: MessageReceivedEvent) = memberInfo(evt, evt.member)

    @Command(description = "Gets information about a specific server member.", locale = Locale.GUILD)
    fun memberInfo(evt: MessageReceivedEvent, member: Member) {
        val title = "${member.user.name}#${member.user.discriminator}" + if (member.nickname != null) {
            " (${member.nickname})"
        } else {
            ""
        }
        val status = member.onlineStatus.name
            .toLowerCase()
            .replace("_", " ")
            .capitalize()
            .plus(if (member.game != null) " - Playing ${member.game?.name}" else "")
        val roles = if (member.roles.isNotEmpty()) {
            member.roles.joinToString(", ") { it.name }
        } else null

        evt.channel.sendEmbed {
            setTitle(title, null)
            setDescription(status)
            setColor(member.color)
            setThumbnail(member.user.effectiveAvatarUrl)
            addField("Joined Discord", member.user.creationTime.format(dateFormat), true)
            addField("Joined this Server", member.joinDate.format(dateFormat), true)
            addField("Do they own the server?", member.isOwner.asYesNo.capitalize(), true)
            addField("Are they a bot?", member.user.isBot.asYesNo.capitalize(), true)
            if (roles != null) addField("Roles", roles, true)
            setFooter("User ID: ${member.user.id}", null)
        }.complete()
    }

    private val Boolean.asYesNo get() = if (this) "yes" else "no"
}
