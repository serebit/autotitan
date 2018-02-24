package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

@Suppress("UNUSED", "TooManyFunctions")
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
        val title = StringBuilder().apply {
            append("${member.user.name}#${member.user.discriminator}")
            member.nickname?.let {
                append(" ($it)")
            }
            if (member.user.isBot) {
                append(" [BOT]")
            }
        }.toString()

        val status = member.onlineStatus.readableName + if (member.game != null) {
            " - Playing ${member.game.name}"
        } else ""

        val roles = member.roles.joinToString(", ") { it.name }

        evt.channel.sendEmbed {
            setTitle(title, null)
            setDescription(status)
            setColor(member.color)
            setThumbnail(member.user.effectiveAvatarUrl)
            val creationDate = member.user.creationTime.format(dateFormat)
            val creationDateDifference = OffsetDateTime.now() - member.user.creationTime
            addField("Joined Discord", "$creationDate ($creationDateDifference)", true)
            val joinDate = member.joinDate.format(dateFormat)
            val joinDateDifference = OffsetDateTime.now() - member.joinDate
            addField("Joined this Server", "$joinDate ($joinDateDifference)", true)
            addField("Do they own the server?", member.isOwner.asYesNo.capitalize(), true)
            if (roles.isNotEmpty()) addField("Roles", roles, true)
            setFooter("User ID: ${member.user.id}", null)
        }.complete()
    }

    private val Boolean.asYesNo get() = if (this) "Yes" else "No"

    private val OnlineStatus.readableName
        get() = name
            .toLowerCase()
            .replace("_", " ")
            .capitalize()

    private val Member.mentionString get() = "${user.name}#${user.discriminator}"

    private operator fun OffsetDateTime.minus(other: Temporal): String {
        val yearDifference = ChronoUnit.YEARS.between(other, this)
        val yearDifferenceString = "$yearDifference year${if (yearDifference != 1L) "s" else ""}"
        val dayDifference = ChronoUnit.DAYS.between(other, minusYears(yearDifference))
        val dayDifferenceString = "$dayDifference day${if (dayDifference != 1L) "s" else ""}"
        return if (yearDifference > 0) {
            "$yearDifferenceString and $dayDifferenceString ago"
        } else "$dayDifferenceString ago"
    }
}
