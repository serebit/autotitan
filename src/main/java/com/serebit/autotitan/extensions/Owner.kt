package com.serebit.autotitan.extensions

import com.serebit.autotitan.Access
import com.serebit.autotitan.annotations.CommandFunction
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
}