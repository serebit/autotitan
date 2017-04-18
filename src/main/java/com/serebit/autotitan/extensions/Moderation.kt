package com.serebit.autotitan.extensions

import com.serebit.autotitan.annotations.GuildCommandFunction
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class Moderation {
  @GuildCommandFunction(
      description = "Bans a member from the current server.",
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
      description = "Bans a user from the current server for the given number of days.",
      permissions = arrayOf(
          Permission.BAN_MEMBERS
      )
  )
  fun tempBan(evt: GuildMessageReceivedEvent, user: User, days: Int) {
    evt.guild.controller.ban(user, days).queue({
      evt.channel.sendMessage("Banned.").queue()
    })
  }
}