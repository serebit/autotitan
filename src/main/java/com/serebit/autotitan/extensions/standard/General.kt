package com.serebit.autotitan.extensions.standard

import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.File
import java.time.format.DateTimeFormatter

@ExtensionClass
class General {
    private val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    @CommandFunction(description = "Pings the bot.")
    fun ping(evt: MessageReceivedEvent) {
        evt.channel.sendMessage("Pong. The last ping was ${evt.jda.ping}ms.").queue()
    }

    @CommandFunction(
            description = "Gets information about the system that the bot is running on."
    )
    fun systemInfo(evt: MessageReceivedEvent) {
        val self = evt.jda.selfUser
        val runtime = Runtime.getRuntime()
        val availableRam = runtime.maxMemory() / 1e6
        val availableRamFormatted = if (availableRam < 1024) {
            "${Math.round(availableRam)}MB"
        } else {
            "${Math.round(availableRam / 1e3)}GB"
        }
        val systemInfo = mapOf(
                "Cores" to runtime.availableProcessors().toString(),
                "Disk Size" to "${Math.round(File("/").totalSpace / 1e9)}GB",
                "Available RAM" to availableRamFormatted
        ).asIterable().joinToString("\n") { "${it.key}: *${it.value}*" }
        val osInfo = mapOf(
                "Name" to System.getProperty("os.name"),
                "Architecture" to System.getProperty("os.arch"),
                "Version" to System.getProperty("os.version")
        ).asIterable().joinToString("\n") { "${it.key}: *${it.value}*" }
        val javaInfo = mapOf(
                "Vendor" to System.getProperty("java.vendor"),
                "Version" to System.getProperty("java.version")
        ).asIterable().joinToString("\n") { "${it.key}: *${it.value}*" }
        val color = evt.guild?.getMember(self)?.color
        val embed = EmbedBuilder().apply {
            setTitle("System Info", null)
            setDescription("Note: These measurements reflect what is available to the JVM.")
            setThumbnail(self.effectiveAvatarUrl)
            setColor(color)
            addField("Hardware", systemInfo, true)
            addField("Operating System", osInfo, true)
            addField("JRE", javaInfo, true)
        }.build()

        evt.channel.sendMessage(embed).queue()
    }

    @CommandFunction(description = "Gets information about the server.", locale = Locale.GUILD)
    fun serverInfo(evt: MessageReceivedEvent) {
        val server = evt.guild
        val canGetInvite = server.selfMember.hasPermission(Permission.MANAGE_SERVER)
        val creationDate = server.creationTime.format(dateFormat)
        val onlineMemberCount = server.members
                .filter { it.onlineStatus == OnlineStatus.ONLINE }
                .size
                .toString()
        val totalMemberCount = server.members.size.toString()
        val textChannelCount = server.textChannels.size.toString()
        val voiceChannelCount = server.voiceChannels.size.toString()
        val guildRoles = server.roles
                .filter { it.name != "@everyone" && it.isHoisted }
                .joinToString(", ") { it.name }
        val embedBuilder = EmbedBuilder().apply {
            setTitle(server.name, null)
            setDescription("Created on $creationDate")
            setThumbnail(server.iconUrl)
            setColor(server.owner.color)
            addField("Owner", server.owner.asMention, true)
            addField("Region", server.region.toString(), true)
            addField("Online Members", onlineMemberCount, true)
            addField("Total Members", totalMemberCount, true)
            addField("Text Channels", textChannelCount, true)
            addField("Voice Channels", voiceChannelCount, true)
            addField("Roles", guildRoles, true)
            setFooter("Server ID: ${server.id}", null)
            if (canGetInvite) {
                val permanentInvites = server.invites.complete().filter { !it.isTemporary }
                if (permanentInvites.isNotEmpty()) addField(
                        "Invite Link",
                        "https://discord.gg/${permanentInvites.first().code}",
                        false
                )
            }
        }.build()

        evt.channel.sendMessage(embedBuilder).queue()
    }

    @CommandFunction(
            description = "Gets information about a specific server member.",
            locale = Locale.GUILD
    )
    fun memberInfo(evt: MessageReceivedEvent, member: Member) {
        val onlineStatus = member.onlineStatus
                .name
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

        evt.channel.sendMessage(embed).queue()
    }
}
