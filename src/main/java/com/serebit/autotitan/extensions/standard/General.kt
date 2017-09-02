package com.serebit.autotitan.extensions.standard

import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import oshi.SystemInfo
import java.time.format.DateTimeFormatter

@ExtensionClass
class General {
    private val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    @CommandFunction(description = "Pings the bot.")
    fun ping(evt: MessageReceivedEvent): Unit = evt.run {
        channel.sendMessage("Pong. The last ping was ${jda.ping}ms.").complete()
    }

    @CommandFunction(
            description = "Gets information about the system that the bot is running on."
    )
    fun systemInfo(evt: MessageReceivedEvent): Unit = evt.run {
        val systemInfo = SystemInfo()
        val hardwareInfo = mapOf(
                "Processor" to systemInfo.hardware.processor.name
                        .replace("(R)", ":registered:")
                        .replace("(TM)", ":tm:"),
                "Motherboard" to systemInfo.hardware.computerSystem.baseboard.model,
                "Disk" to systemInfo.hardware.diskStores[0].model
                        .replace("(R)", ":registered:")
                        .replace("(TM)", ":tm:")
                        .replace("_", " "),
                "Total Memory" to humanReadableByteCount(systemInfo.hardware.memory.total)
        ).asIterable().joinToString("\n") { "**${it.key}**: ${it.value}" }
        val osInfo = mapOf(
                "Name" to systemInfo.operatingSystem.family,
                "Version" to systemInfo.operatingSystem.version
        ).asIterable().joinToString("\n") { "**${it.key}**: ${it.value}" }
        val javaInfo = mapOf(
                "Vendor" to System.getProperty("java.vendor"),
                "Version" to System.getProperty("java.version")
        ).asIterable().joinToString("\n") { "**${it.key}**: ${it.value}" }

        val embed = EmbedBuilder().apply {
            setColor(guild?.getMember(jda.selfUser)?.color)
            addField("Hardware", hardwareInfo, true)
            addField("Operating System", osInfo, true)
            addField("JRE", javaInfo, true)
        }.build()

        channel.sendMessage(embed).complete()
    }

    @CommandFunction(description = "Gets information about the server.", locale = Locale.GUILD)
    fun serverInfo(evt: MessageReceivedEvent): Unit = evt.run {
        val canGetInvite = guild.selfMember.hasPermission(Permission.MANAGE_SERVER)
        val creationDate = guild.creationTime.format(dateFormat)
        val onlineMemberCount = guild.members
                .filter { it.onlineStatus == OnlineStatus.ONLINE }
                .size
                .toString()
        val totalMemberCount = guild.members.size.toString()
        val textChannelCount = guild.textChannels.size.toString()
        val voiceChannelCount = guild.voiceChannels.size.toString()
        val hoistedRoles = guild.roles
                .filter { it.name != "@everyone" && it.isHoisted }
                .joinToString(", ") { it.name }
        val embedBuilder = EmbedBuilder().apply {
            setTitle(guild.name, null)
            setDescription("Created on $creationDate")
            setThumbnail(guild.iconUrl)
            setColor(guild.owner.color)
            addField("Owner", guild.owner.asMention, true)
            addField("Region", guild.region.toString(), true)
            addField("Online Members", onlineMemberCount, true)
            addField("Total Members", totalMemberCount, true)
            addField("Text Channels", textChannelCount, true)
            addField("Voice Channels", voiceChannelCount, true)
            addField("Hoisted Roles", hoistedRoles, true)
            if (canGetInvite) {
                val permanentInvites = guild.invites.complete().filter { !it.isTemporary }
                if (permanentInvites.isNotEmpty()) addField(
                        "Invite Link",
                        "https://discord.gg/${permanentInvites.first().code}",
                        false
                )
            }
            setFooter("Server ID: ${guild.id}", null)
        }.build()

        channel.sendMessage(embedBuilder).complete()
    }

    @CommandFunction(
            description = "Gets information about the invoker.",
            locale = Locale.GUILD
    )
    fun selfInfo(evt: MessageReceivedEvent) = memberInfo(evt, evt.member)

    @CommandFunction(
            description = "Gets information about a specific server member.",
            locale = Locale.GUILD
    )
    fun memberInfo(evt: MessageReceivedEvent, member: Member): Unit = evt.run {
        val onlineStatus = member.onlineStatus.name
                .toLowerCase()
                .replace("_", " ")
                .capitalize()
        val roles = if (member.roles.isNotEmpty()) {
            member.roles.joinToString(", ") { it.name }
        } else "None"

        val embed = EmbedBuilder().apply {
            setTitle(member.effectiveName, null)
            setDescription(onlineStatus + if (member.game != null) " - Playing ${member.game}" else "")
            setColor(member.color)
            setThumbnail(member.user.effectiveAvatarUrl)
            addField("Joined Discord", member.user.creationTime.format(dateFormat), true)
            addField("Joined this Server", member.joinDate.format(dateFormat), true)
            addField("Roles", roles, true)
            setFooter("User ID: ${member.user.id}", null)
        }.build()

        channel.sendMessage(embed).complete()
    }

    private fun humanReadableByteCount(bytes: Long): String {
        val exponent = (Math.log(bytes.toDouble()) / 6.9).toInt()
        val unit = listOf("B", "kB", "MB", "GB", "TB", "PB", "EB")[exponent]
        return "%.1f $unit".format(bytes / Math.pow(1000.0, exponent.toDouble()))
    }
}
