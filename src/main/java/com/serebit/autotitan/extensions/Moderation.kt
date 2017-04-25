package com.serebit.autotitan.extensions

import com.serebit.autotitan.annotations.GuildCommandFunction
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class Moderation {
  @GuildCommandFunction(
      description = "Kicks a member from the current server.",
      permissions = arrayOf(
          Permission.KICK_MEMBERS
      )
  )
  fun kick(evt: GuildMessageReceivedEvent, member: Member) {
    evt.guild.controller.kick(member).queue({
      evt.channel.sendMessage("Kicked.").queue()
    })
  }

  @GuildCommandFunction(
      description = "Bans a member from the current server.",
      permissions = arrayOf(
          Permission.BAN_MEMBERS
      )
  )
  fun ban(evt: GuildMessageReceivedEvent, member: Member) {
    evt.guild.controller.ban(member, 0).queue({
      evt.channel.sendMessage("Banned.").queue()
    })
  }
  
  @GuildCommandFunction(
      description = "Bans a member from the current server, and deletes 7 days worth of their messages.",
      permissions = arrayOf(
          Permission.BAN_MEMBERS
      )
  )
  fun hardBan(evt: GuildMessageReceivedEvent, member: Member) {
    evt.guild.controller.ban(member, 7).queue({
      evt.channel.sendMessage("Banned.").queue()
    })
  }
  
  @GuildCommandFunction(
      description = "Unbans a banned user from the current server.",
      permissions = arrayOf(
          Permission.BAN_MEMBERS
      )
  )
  fun unBan(evt: GuildMessageReceivedEvent, user: User) {
    evt.guild.controller.unban(user).queue({
      evt.channel.sendMessage("Unbanned.").queue()
    })
  }

  
}