package com.serebit.autotitan

import com.serebit.autotitan.data.commands
import com.serebit.autotitan.data.listeners
import com.serebit.autotitan.extensions.jda
import com.serebit.autotitan.listeners.EventListener
import net.dv8tion.jda.core.AccountType

const val NAME = "Spyglass"
const val VERSION = "0.3.1"

fun main(args: Array<String>) {
    val jda = jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener(commands, listeners))
    }

    println()
    println("$NAME v$VERSION")
    println("Username:    ${jda.selfUser.name}")
    println("Ping:        ${jda.ping}ms")
    println("Invite link: ${jda.asBot().getInviteUrl()}")
}
