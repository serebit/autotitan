package com.serebit.autotitan.modules

import com.serebit.autotitan.api.ModuleTemplate
import com.serebit.autotitan.api.extensions.jda.isBotOwner
import com.serebit.autotitan.api.extensions.jda.sendEmbed
import com.serebit.autotitan.api.meta.Access
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.math.absoluteValue

@Suppress("UNUSED")
class General : ModuleTemplate() {
    private val dateFormat = DateTimeFormatter.ofPattern("d MMM, yyyy")

    init {
        command("ping", "Pings the bot.") {
            it.channel.sendMessage("Pong. The last ping was ${it.jda.ping}ms.").queue()
        }

        command("serverInfo", "Gets information about the server.", Access.Guild.All()) { evt ->
            val onlineMemberCount = evt.guild.members.count { it.onlineStatus != OnlineStatus.OFFLINE }
            val hoistedRoles = evt.guild.roles
                .asSequence()
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
            }.queue()
        }

        command("selfInfo", "Gets information about the invoker.") { sendMemberInfo(it, it.member) }

        command(
            "memberInfo",
            "Gets information about a specific server member.",
            Access.Guild.All(),
            task = ::sendMemberInfo
        )

        command("invite", "Sends the bot's invite link.") { evt ->
            val inviteMessage = """
                Invite link: <${evt.jda.asBot().getInviteUrl()}>. Remember, you need Manage Server permissions to
                add me to a server.
                """.trimIndent()
            evt.jda.asBot().applicationInfo.queue { applicationInfo ->
                when {
                    applicationInfo.isBotPublic ->
                        evt.channel.sendMessage(inviteMessage).queue()
                    evt.author.isBotOwner -> evt.author.openPrivateChannel().queue { channel ->
                        evt.channel.sendMessage("Sent the invite link in PMs.").queue()
                        channel.sendMessage(inviteMessage).queue()
                    }
                    else -> evt.channel.sendMessage(
                        "I'm set to private. Contact the bot owner if you want me to join your server."
                    ).queue()
                }
            }
        }
    }

    private fun sendMemberInfo(evt: MessageReceivedEvent, member: Member) {
        val title = buildString {
            append("${member.user.name}#${member.user.discriminator}")
            member.nickname?.let { append(" ($it)") }
            if (member.user.isBot) append(" [BOT]")
        }

        val status = buildString {
            append(member.onlineStatus.readableName)
            member.game?.let { append(" - Playing ${member.game.name}") }
        }

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
            if (member.roles.isNotEmpty()) {
                addField("Roles", member.roles.joinToString(", ") { it.name }, true)
            }
            setFooter("User ID: ${member.user.id}", null)
        }.queue()
    }

    private val Boolean.asYesNo get() = if (this) "Yes" else "No"

    private val OnlineStatus.readableName
        get() = name.toLowerCase()
            .replace("_", " ")
            .capitalize()

    private val Member.mentionString get() = "${user.name}#${user.discriminator}"

    private operator fun OffsetDateTime.minus(other: Temporal): String {
        val yearDifference = ChronoUnit.YEARS.between(other, this)
        val yearDifferenceString = buildString {
            append("$yearDifference year")
            if (yearDifference.absoluteValue != 1L) append("s")
        }
        val dayDifference = ChronoUnit.DAYS.between(other, minusYears(yearDifference))
        val dayDifferenceString = buildString {
            append("$dayDifference day")
            if (dayDifference.absoluteValue != 1L) append("s")
        }
        return if (yearDifference > 0) {
            "$yearDifferenceString and $dayDifferenceString ago"
        } else "$dayDifferenceString ago"
    }
}
