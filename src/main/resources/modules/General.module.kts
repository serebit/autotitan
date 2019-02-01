import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.math.absoluteValue

// has to be suspend for the event listener to accept it as a valid function
@Suppress("RedundantSuspendModifier")
suspend fun sendMemberInfo(evt: MessageReceivedEvent, member: Member) {
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
        val creationDate = member.user.creationTime.format(DateTimeFormatter.ISO_DATE)
        val creationDateDifference = OffsetDateTime.now() - member.user.creationTime
        addField("Joined Discord", "$creationDate ($creationDateDifference)", true)
        val joinDate = member.joinDate.format(DateTimeFormatter.ISO_DATE)
        val joinDateDifference = OffsetDateTime.now() - member.joinDate
        addField("Joined this Server", "$joinDate ($joinDateDifference)", true)
        addField("Do they own the server?", member.isOwner.asYesNo.capitalize(), true)
        if (member.roles.isNotEmpty()) {
            addField("Roles", member.roles.joinToString(", ") { it.name }, true)
        }
        setFooter("User ID: ${member.user.id}", null)
    }.queue()
}

val Boolean.asYesNo get() = if (this) "Yes" else "No"

val OnlineStatus.readableName
    get() = name.toLowerCase()
        .replace("_", " ")
        .capitalize()

operator fun OffsetDateTime.minus(other: Temporal): String {
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

module("General") {
    command("ping", "Pings the bot.") {
        channel.sendMessage("Pong. The last ping was ${jda.ping}ms.").queue()
    }

    command("serverInfo", "Gets information about the server.", Access.Guild.All()) {
        val onlineMemberCount = guild.members.count { it.onlineStatus != OnlineStatus.OFFLINE }
        val hoistedRoles = guild.roles
            .asSequence()
            .filter { it.name != "@everyone" && it.isHoisted }
            .joinToString(", ") { it.name }

        channel.sendEmbed {
            setTitle(guild.name, null)
            setDescription("Created on ${guild.creationTime.format(DateTimeFormatter.ISO_DATE)}")
            setThumbnail(guild.iconUrl)
            addField("Owner", guild.owner.asMention, true)
            addField("Region", guild.region.toString(), true)
            addField("Online Members", onlineMemberCount.toString(), true)
            addField("Total Members", guild.members.size.toString(), true)
            addField("Bots", guild.members.count { it.user.isBot }.toString(), true)
            addField("Text Channels", guild.textChannels.size.toString(), true)
            addField("Voice Channels", guild.voiceChannels.size.toString(), true)
            addField("Hoisted Roles", hoistedRoles, true)
            if (guild.selfMember.hasPermission(Permission.MANAGE_SERVER)) {
                val permanentInvites = guild.invites.complete().filter { !it.isTemporary }
                if (permanentInvites.isNotEmpty()) addField(
                    "Invite Link",
                    permanentInvites.first().url,
                    false
                )
            }
            setFooter("Server ID: ${guild.id}", null)
        }.queue()
    }

    command("selfInfo", "Gets information about the invoker.") { sendMemberInfo(this, member) }

    command(
        "memberInfo",
        "Gets information about a specific server member.",
        Access.Guild.All(),
        task = ::sendMemberInfo
    )

    command("invite", "Sends the bot's invite link.") {
        val inviteMessage = """
            Invite link: <${jda.asBot().getInviteUrl()}>.
            Remember, you need Manage Server permissions to add me to a server.
        """.trimIndent()
        jda.asBot().applicationInfo.queue { applicationInfo ->
            when {
                applicationInfo.isBotPublic ->
                    channel.sendMessage(inviteMessage).queue()
                author.isBotOwner -> author.openPrivateChannel().queue { channel ->
                    channel.sendMessage("Sent the invite link in PMs.").queue()
                    channel.sendMessage(inviteMessage).queue()
                }
                else -> channel.sendMessage(
                    "I'm set to private. Contact the bot owner if you want me to join your server."
                ).queue()
            }
        }
    }
}
