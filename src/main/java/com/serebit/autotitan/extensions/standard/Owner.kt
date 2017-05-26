package com.serebit.autotitan.extensions.standard

import com.serebit.autotitan.api.Access
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.data.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Owner {
  @CommandFunction(
      description = "Shuts down the bot with an exit code of 0.",
      access = Access.BOT_OWNER
  )
  fun shutDown(evt: MessageReceivedEvent) {
    with(evt) {
      channel.sendMessage("Shutting down.").complete()
      jda.shutdown()
      System.exit(0)
    }
  }

  @CommandFunction(
      description = "Renames the bot.",
      delimitFinalParameter = false,
      access = Access.BOT_OWNER
  )
  fun setName(evt: MessageReceivedEvent, name: String) {
    with(evt) {
      jda.selfUser.manager.setName(name).complete()
      channel.sendMessage("Renamed to $name.").queue()
    }
  }

  @CommandFunction(
      description = "Changes the bot's command prefix.",
      access = Access.BOT_OWNER
  )
  fun setPrefix(evt: MessageReceivedEvent, prefix: String) {
    Command.prefix = prefix
    evt.channel.sendMessage("Set prefix to `${Command.prefix}`.").queue()
  }
  
  @CommandFunction(
      description = "Sends the bot's invite link to the command invoker.",
      access = Access.BOT_OWNER
  )
  fun getInvite(evt: MessageReceivedEvent) {
    with(evt) {
      author.openPrivateChannel().complete().sendMessage(
          "Invite link: ${jda.asBot().getInviteUrl()}"
      ).queue()
    }
  }
  
  @CommandFunction(
      description = "Gets the list of servers that the bot is currently in.",
      access = Access.BOT_OWNER
  )
  fun serverList(evt: MessageReceivedEvent) {
    val color = if(guild != null) {
      evt.guild.selfMember.color
    } else null
    val embedBuilder = EmbedBuilder()
    with(embedBuilder) {
      setTitle("Server List", null)
      setDescription("A complete list of all the servers that I'm in.")
      setThumbnail(evt.jda.selfUser.effectiveAvatarUrl)
      setColor(color)
      evt.jda.guilds.forEach {
        addField(it.name, "Server ID: ${it.id}")
      }
    }
    evt.channel.sendMessage(embedBuilder.build()).queue()
  }
  
  @CommandFunction(
      description = "Leaves the server.",
      access = Access.BOT_OWNER
  )
  fun leaveServer(evt: MessageReceivedEvent) {
    with(evt) {
      channel.sendMessage("Leaving the server.").complete()
      guild.leave().complete()
    }
  }
}
