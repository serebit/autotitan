package com.serebit.autotitan

import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import net.dv8tion.jda.core.AccountType
import org.slf4j.event.Level
import org.slf4j.simple.SimpleLogger

const val NAME = "AutoTitan"
const val VERSION = "0.4.0"
val config: Configuration = Configuration.generate()

fun main(args: Array<String>) {
    args.find { it in setOf("-d", "--debug") }?.let {
        setLoggingLevel(Level.DEBUG)
    }

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

fun setLoggingLevel(level: Level) {
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, level.toString())
}