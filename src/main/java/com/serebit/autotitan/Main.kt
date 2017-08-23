package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.config.Configuration
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Extension
import com.serebit.autotitan.data.Listener
import com.serebit.autotitan.listeners.EventListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder

const val name = "autotitan"
const val version = "0.1.0"
private val extensions = ClassPath.from(Thread.currentThread().contextClassLoader)
        .getTopLevelClassesRecursive("com.serebit.autotitan.extensions")
        .map { it.load() }

fun main(args: Array<String>) {
    val jda = JDABuilder(AccountType.BOT).apply {
        val commands = extensions.mapNotNull { clazz ->
            val instance = Extension.generate(clazz)
            if (instance != null) {
                clazz.methods.mapNotNull { Command.generate(instance, it) }
            } else null
        }.flatten().toSet()
        val listeners = extensions.mapNotNull { clazz ->
            val instance = Extension.generate(clazz)
            if (instance != null) {
                clazz.methods.mapNotNull { Listener.generate(instance, it) }
            } else null
        }.flatten().toSet()
        setToken(Configuration.token)
        addEventListener(EventListener(commands, listeners))
    }.buildBlocking()

    println()
    println("Username:    ${jda.selfUser.name}")
    println("Ping:        ${jda.ping}ms")
    println("Invite link: ${jda.asBot().getInviteUrl()}")
}
