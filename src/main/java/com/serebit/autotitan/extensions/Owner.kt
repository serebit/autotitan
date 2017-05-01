package com.serebit.autotitan.extensions

import com.serebit.autotitan.Access
import com.serebit.autotitan.annotations.CommandFunction
import com.serebit.autotitan.data.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

/**
 * Created by gingerdeadshot on 4/28/17.
 */
class Owner {
  @CommandFunction(
      description = "Shuts down the bot.",
      access = Access.BOT_OWNER
  )
  fun shutDown(evt: MessageReceivedEvent) {
    evt.textChannel.sendMessage("Shutting down.").queue {
      evt.jda.shutdown()
      System.exit(0)
    }
  }

  @CommandFunction(
      description = "Renames the bot.",
      delimitFinalParameter = false,
      access = Access.BOT_OWNER
  )
  fun rename(evt: MessageReceivedEvent, name: String) {
    evt.jda.selfUser.manager.setName(name).queue({
      evt.channel.sendMessage("Renamed to $name.").queue()
    }, {
      evt.author.privateChannel.sendMessage("```\n${it.message}\n```").queue()
    })
  }

  @CommandFunction(
      description = "Changes the bot's command prefix.",
      access = Access.BOT_OWNER
  )
  fun setPrefix(evt: MessageReceivedEvent, prefix: String) {
    Command.prefix = prefix
    evt.channel.sendMessage("Set prefix to `${Command.prefix}`.").queue()
  }
}