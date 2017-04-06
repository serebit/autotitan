package extensions

import annotation.Command
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class General {
  @Command(description = "Pings the bot.")
  fun ping(evt: MessageReceivedEvent) {
    evt.channel.sendMessage("Pong.").queue()
  }
  
  @Command(description = "Gets information about a specific server member.")
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
      name = "Joined Discord on",
      value = user.creationTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
      inline = true
    )
    embedBuilder.addField(
      name = "Joined this server on",
      value = member.joinDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
      inline = true
    )
    embedBuilder.addField(
      name = "Roles"
      value = member.roles.toMutableList().joinToString(", "),
      inline = true
  }
}
