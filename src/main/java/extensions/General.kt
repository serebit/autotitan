package extensions

import annotations.Command
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.format.DateTimeFormatter

class General {
  @Command(description = "Pings the bot.")
  fun ping(evt: MessageReceivedEvent) {
    evt.channel.sendMessage("Pong.").queue()
  }

  @Command(description = "Gets information about the server.", serverOnly = true)
  fun serverInfo(evt: MessageReceivedEvent) {
    val server = evt.guild
    val embedBuilder = EmbedBuilder()
    embedBuilder.setTitle(server.name, null)
    embedBuilder.setDescription(
        "Created on " + server.creationTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
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
    embedBuilder.setFooter(
        "Server ID: " + server.id,
        null
    )
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }

  @Command(description = "Gets information about the member who called the command.", serverOnly = true)
  fun memberInfo(evt: MessageReceivedEvent) {

  }

  @Command(description = "Gets information about a specific server member.", serverOnly = true)
  fun memberInfo(evt: MessageReceivedEvent, member: Member) {
    val user = member.user
    val embedBuilder = EmbedBuilder()
    var title = user.name + "#" + user.discriminator
    if (member.nickname != null) {
      title += "(also known as " + member.nickname + ")"
    }
    embedBuilder.setTitle(title, null)
    embedBuilder.setDescription("User ID: " + user.id)
    embedBuilder.setColor(member.color)
    embedBuilder.setThumbnail(user.effectiveAvatarUrl)
    embedBuilder.addField(
        "Joined Discord on",
        user.creationTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        true
    )
    embedBuilder.addField(
        "Joined this server on",
        member.joinDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        true
    )
    embedBuilder.addField(
        "Roles",
        member.roles.toMutableList().joinToString(", "),
        true
    )
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }
}
