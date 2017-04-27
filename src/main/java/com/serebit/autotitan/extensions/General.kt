package com.serebit.autotitan.extensions

import com.serebit.autotitan.Locale
import com.serebit.autotitan.annotations.CommandFunction
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.format.DateTimeFormatter

class General {
  @CommandFunction(description = "Pings the bot.")
  fun ping(evt: MessageReceivedEvent) {
    evt.channel.sendMessage("Pong. The last ping was ${evt.jda.ping}ms.").queue()
  }

  @CommandFunction(description = "Gets information about the server.", locale = Locale.GUILD)
  fun serverInfo(evt: GuildMessageReceivedEvent) {
    val server = evt.guild
    val permanentInvites = server.invites.complete(true).filter { !it.isTemporary }
    val invite = when (permanentInvites.isNotEmpty()) {
      true -> "https://discord.gg/${permanentInvites.first().code}"
      false -> null
    }
    val creationDate = server.creationTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    val onlineMemberCount = server.members
        .filter { it.onlineStatus == OnlineStatus.ONLINE }
        .size
        .toString()
    val totalMemberCount = server.members.size.toString()
    val textChannelCount = server.textChannels.size.toString()
    val voiceChannelCount = server.voiceChannels.size.toString()
    val guildRoles = server.roles
        .filter { it.name != "@everyone" }
        .map { it.name }
        .joinToString(", ")
    val embedBuilder = EmbedBuilder()
        .setTitle(server.name, invite)
        .setDescription(creationDate)
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
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }

  @GuildCommandFunction(description = "Gets information about a specific server member.")
  fun memberInfo(evt: GuildMessageReceivedEvent, member: Member) {
    val user = member.user
    val onlineStatus = member.onlineStatus
        .name
        .toLowerCase()
        .split("_")
        .map(String::capitalize)
        .joinToString(" ")
    val gameName = when(member.game != null) {
      true -> member.game.name
      false -> ""
    }
    val description = onlineStatus + when(gameName != "") {
      true -> " - Playing $gameName"
      false -> ""
    }
    val discordJoinDate = user.creationTime.format(
        DateTimeFormatter.ofPattern("h:m a 'on' MMMM d, yyyy")
    )
    val serverJoinDate = member.joinDate.format(
        DateTimeFormatter.ofPattern("h:m a 'on' MMMM d, yyyy")
    )
    val roles = member.roles.map { it.name }.joinToString(", ")
    val embedBuilder = EmbedBuilder()
        .setTitle(member.asMention, null)
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
