package com.serebit.autotitan.extensions

import com.serebit.autotitan.annotations.GuildCommandFunction
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class Moderation {
  @GuildCommandFunction(
      description = "Kicks a user from the current server.",
      permissions = arrayOf(
          Permission.KICK_MEMBERS
      )
  )
  fun kick(evt: GuildMessageReceivedEvent, user: User) {
    evt.guild.controller.kick(user).queue({ 
      evt.channel.sendMessage("Kicked.").queue()
    })
  }

  @GuildCommandFunction(
      description = "Bans a user from the current server.",
      permissions = arrayOf(
          Permission.BAN_MEMBERS
      )
  )
  fun ban(evt: GuildMessageReceivedEvent, user: User) {
    evt.guild.controller.ban(user, 0).queue({ 
      evt.channel.sendMessage("Banned.").queue()
    })
  }
  
  @GuildCommandFunction(
      description = "Bans a user from the current server, and deletes 7 days worth of their messages.",
      permissions = arrayOf(
          Permission.BAN_MEMBERS
      )
  )
  fun hardBan(evt: GuildMessageReceivedEvent, user: User) {
    evt.guild.controller.ban(user, 7).queue({ 
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
    evt.guild.controller.unBan(user, 0).queue({ 
      evt.channel.sendMessage("Unbanned.").queue()
    })
  }

  
}