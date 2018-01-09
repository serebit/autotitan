package com.serebit.autotitan

import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import net.dv8tion.jda.core.AccountType

const val NAME = "AutoTitan"
const val VERSION = "0.4.0"
val config: Configuration = Configuration.generate()

fun main(args: Array<String>) {
    jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener)
    }.let {
        println()
        println("$NAME v$VERSION")
        println("Username:    ${it.selfUser.name}")
        println("Ping:        ${it.ping}ms")
        println("Invite link: ${it.asBot().getInviteUrl()}")
    }
}