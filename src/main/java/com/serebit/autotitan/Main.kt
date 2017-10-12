package com.serebit.autotitan

import com.serebit.autotitan.data.commands
import com.serebit.autotitan.data.listeners
import com.serebit.autotitan.listeners.EventListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder

const val NAME = "AutoTitan"
const val VERSION = "0.2.2"

fun main(args: Array<String>) {
    val jda = JDABuilder(AccountType.BOT).apply {
        setToken(config.token)
        addEventListener(EventListener(commands, listeners))
    }.buildBlocking()

    println("$NAME v$VERSION")
    println("Username:    ${jda.selfUser.name}")
    println("Ping:        ${jda.ping}ms")
    println("Invite link: ${jda.asBot().getInviteUrl()}")
}
