package com.serebit.autotitan.extensions

import com.serebit.autotitan.Locale
import com.serebit.autotitan.annotations.CommandFunction
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.format.DateTimeFormatter

class General {
  private val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy")
  
  @CommandFunction(description = "Pings the bot.")
  fun ping(evt: MessageReceivedEvent) {
    evt.channel.sendMessage("Pong. The last ping was ${evt.jda.ping}ms.").queue()
  }
  
  @CommandFunction(description = "Gets information about the bot.")
  fun info(evt: MessageReceivedEvent) {
    val self = evt.jda.selfUser
    val applicationInfo = evt.jda.asBot().getApplicationInfo().complete()
    val creationDate = self.creationTime.format(dateFormat)
    val operatingSystem = System.getProperty("os.name")
    val architecture = System.getProperty("os.arch")
    val javaVendor = System.getProperty("java.vendor")
    val javaVersion = System.getProperty("java.version")
    val embedBuilder = EmbedBuilder()
        .setTitle(self.name, null)
        .setDescription(applicationInfo.description)
        .setThumbnail(self.effectiveAvatarUrl)
        .addField("Owner", applicationInfo.owner.name, true)
        .addField("Operating System", "$operatingSystem $architecture", true)
        .addField("Java Version", "$javaVendor $javaVersion", true)
    evt.channel.sendMessage(embedBuilder.build()).queue()
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
        .map { it.name }
        .joinToString(", ")
    val embedBuilder = EmbedBuilder()
        .setTitle(server.name, null)
        .setDescription("Created on $creationDate")
        .setThumbnail(server.iconUrl)
        .setColor(server.owner.color)
        .addField("Owner", server.owner.asMention, true)
        .addField("Region", server.region.toString(), true)
        .addField("Online Members", onlineMemberCount, true)
        .addField("Total Members", totalMemberCount, true)
        .addField("Text Channels", textChannelCount, true)
        .addField("Voice Channels", voiceChannelCount, true)
        .addField("Roles", guildRoles, true)
        .setFooter("Server ID: ${server.id}", null)
    if (canGetInvite) {
      val permanentInvites = server.invites.complete().filter { !it.isTemporary }
      if (permanentInvites.isNotEmpty()) {
        val inviteUrl = "https://discord.gg/${permanentInvites.first().code}"
        embedBuilder.addField("Invite Link", inviteUrl, false)
      }
    }
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }

  @CommandFunction(
      description = "Gets information about a specific server member.",
      locale = Locale.GUILD
  )
  fun memberInfo(evt: MessageReceivedEvent, member: Member) {
    val user = member.user
    val onlineStatus = member.onlineStatus
        .name
        .toLowerCase()
        .replace("_", " ")
        .capitalize()
    val description = onlineStatus + if (member.game != null) {
      " - Playing ${member.game}"
    } else ""
    val discordJoinDate = user.creationTime.format(datePattern)
    val serverJoinDate = member.joinDate.format(datePattern)
    val roles = if (member.roles.isNotEmpty()) {
      member.roles.map { it.name }.joinToString(", ")
    } else "None"
    val embedBuilder = EmbedBuilder()
        .setTitle(member.effectiveName, null)
        .setDescription(description)
        .setColor(member.color)
        .setThumbnail(user.effectiveAvatarUrl)
        .addField("Joined Discord", discordJoinDate, true)
        .addField("Joined this Server", serverJoinDate, true)
        .addField("Roles", roles, true)
        .setFooter("User ID: " + user.id, null)
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }
}
