package com.serebit.autotitan.extensions

import com.serebit.autotitan.Locale
import com.serebit.autotitan.annotations.CommandFunction
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Moderation {
  @CommandFunction(
      description = "Kicks a member from the current server.",
      locale = Locale.GUILD,
      permissions = arrayOf(Permission.KICK_MEMBERS)
  )
  fun kick(evt: MessageReceivedEvent, member: Member) {
    evt.guild.controller.kick(member).queue({
      evt.channel.sendMessage("Kicked.").queue()
    })
  }
  
  @CommandFunction(
      description = "Kicks a member from the server and deletes 1 day's worth of their messages.",
      locale = Locale.GUILD,
      permissions = arrayOf(Permission.BAN_MEMBERS)
  )
  fun softBan(evt: MessageReceivedEvent, member: Member) {
    evt.guild.controller.ban(member, 1).queue({
      evt.guild.controller.unban(member.user).queue({
        evt.channel.sendMessage("Softbanned.").queue()
      }
    }
  }

  @CommandFunction(
      description = "Bans a member from the current server.",
      locale = Locale.GUILD,
      permissions = arrayOf(Permission.BAN_MEMBERS)
  )
  fun ban(evt: MessageReceivedEvent, member: Member) {
    evt.guild.controller.ban(member, 0).queue({
      evt.channel.sendMessage("Banned.").queue()
    })
  }
  
  @CommandFunction(
      description = "Bans a member from the current server, and deletes 7 days worth of their messages.",
      locale = Locale.GUILD,
      permissions = arrayOf(Permission.BAN_MEMBERS)
  )
  fun hardBan(evt: MessageReceivedEvent, member: Member) {
    evt.guild.controller.ban(member, 7).queue({
      evt.channel.sendMessage("Banned.").queue()
    })
  }
  
  @CommandFunction(
      description = "Unbans a banned user from the current server.",
      locale = Locale.GUILD,
      permissions = arrayOf(Permission.BAN_MEMBERS)
  )
  fun unBan(evt: MessageReceivedEvent, user: User) {
    evt.guild.controller.unban(user).queue({
      evt.channel.sendMessage("Unbanned.").queue()
    })
  }

  @CommandFunction(
      description = "Deletes the last N messages in the channel. N must be in the range of 1..99.",
      locale = Locale.GUILD,
      permissions = arrayOf(Permission.MESSAGE_MANAGE)
  )
  fun cleanUp(evt: MessageReceivedEvent, number: Int) {
    if (number in (1..99)) {
      evt.textChannel.history.retrievePast(number + 1).queue({
        evt.textChannel.deleteMessages(it).queue()
      })
    }
  }
}