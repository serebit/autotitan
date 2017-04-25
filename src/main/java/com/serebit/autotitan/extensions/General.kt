package com.serebit.autotitan.extensions

import com.serebit.autotitan.Access
import com.serebit.autotitan.annotations.CommandFunction
import com.serebit.autotitan.annotations.GuildCommandFunction
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.format.DateTimeFormatter

class General {
  @CommandFunction(description = "Pings the bot.")
  fun ping(evt: MessageReceivedEvent) {
    evt.channel.sendMessage("Pong. The current ping is ${evt.jda.ping}ms.").queue()
  }

  @CommandFunction(description = "Gets information about the server.", access = Access.GUILD_ONLY)
  fun serverInfo(evt: GuildMessageReceivedEvent) {
    val server = evt.guild
    val embedBuilder = EmbedBuilder()
    val permanentInvites = server.invites.complete(true).filter { !it.isTemporary }
    var invite: String? = null
    if (permanentInvites.isNotEmpty()) {
      invite = "https://discord.gg/${permanentInvites.first().code}"
    }
    embedBuilder.setTitle(server.name, invite)
    embedBuilder.setDescription(
        "Created on ${server.creationTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))}"
    )
    if (server.iconUrl != null) {
      embedBuilder.setThumbnail(server.iconUrl)
    }
    embedBuilder.setColor(server.owner.color)
    embedBuilder.addField(
        "Owner",
        server.owner.asMention,
        true
    )
    embedBuilder.addField(
        "Region",
        server.region.toString(),
        true
    )
    embedBuilder.addField(
        "Online Members",
        server.members.filter { it.onlineStatus == OnlineStatus.ONLINE }.size.toString(),
        true
    )
    embedBuilder.addField(
        "Total Members",
        server.members.size.toString(),
        true
    )
    embedBuilder.addField(
        "Text Channels",
        server.textChannels.size.toString(),
        true
    )
    embedBuilder.addField(
        "Voice Channels",
        server.voiceChannels.size.toString(),
        true
    )
    embedBuilder.addField(
        "Roles",
        server.roles
            .filter { it.name != "@everyone" }
            .map { it.name }
            .joinToString(", "),
        false
    )
    embedBuilder.setFooter(
        "Server ID: ${server.id}",
        null
    )
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }

  @GuildCommandFunction(description = "Gets information about a specific server member.")
  fun memberInfo(evt: GuildMessageReceivedEvent, member: Member) {
    val user = member.user
    val embedBuilder = EmbedBuilder()
    var title = user.name + "#" + user.discriminator
    if (member.nickname != null) {
      title += " - ${member.nickname}"
    }
    var onlineStatus = member.onlineStatus.name.toLowerCase().split("_").map(String::capitalize).joinToString(" ")
    var description = "$onlineStatus"
    if (member.game != null) {
      description += " - Playing ${member.game.name}"
    }
    embedBuilder.setDescription(description)
    embedBuilder.setTitle(title, null)
    embedBuilder.setColor(member.color)
    embedBuilder.setThumbnail(user.effectiveAvatarUrl)
    embedBuilder.addField(
        "Joined Discord",
        user.creationTime.format(DateTimeFormatter.ofPattern("h:m a 'on' MMMM d, yyyy")),
        true
    )
    embedBuilder.addField(
        "Joined this Server",
        member.joinDate.format(DateTimeFormatter.ofPattern("h:m a 'on' MMMM d, yyyy")),
        true
    )
    embedBuilder.addField(
        "Roles",
        member.roles.map { it.name }.joinToString(", "),
        true
    )
    embedBuilder.setFooter("User ID: " + user.id, null)
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }
}
