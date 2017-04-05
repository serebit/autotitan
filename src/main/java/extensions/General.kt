package extensions

import annotation.Command
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class General {
  @Command()
  fun ping(evt: MessageReceivedEvent) {
    evt.channel.sendMessage("Pong.").queue()
  }
  
  @Command()
  fun memberInfo(evt: MessageReceivedEvent, member: Member) {
    val user = member.user
    val embedBuilder = EmbedBuilder()
    var title = "User info for " + user.name + user.discriminator
    if (member.nickname != null) title += " (also known as " + member.nickname + ")"
    embedBuilder.setTitle(title, null)
    embedBuilder.setColor(member.color)
    embedBuilder.setThumbnail(user.effectiveAvatarUrl)
  }
}
