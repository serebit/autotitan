package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Extension
import com.serebit.autotitan.data.Listener
import com.serebit.autotitan.listeners.EventListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder

const val name = "AutoTitan"
const val version = "0.2.2"

fun main(args: Array<String>) {
    val jda = JDABuilder(AccountType.BOT).apply {
        val extensions = ClassPath.from(Thread.currentThread().contextClassLoader)
                .getTopLevelClassesRecursive("com.serebit.autotitan.extensions")
                .mapNotNull { Extension.generate(it.load()) }
        val commands = extensions.map { instance ->
            instance::class.java.methods.mapNotNull { Command.generate(instance, it) }
        }.flatten().toSet()
        val listeners = extensions.map { instance ->
            instance::class.java.methods.mapNotNull { Listener.generate(instance, it) }
        }.flatten().toSet()
        setToken(config.token)
        addEventListener(EventListener(commands, listeners))
    }.buildBlocking()

    println()
    println("Username:    ${jda.selfUser.name}")
    println("Ping:        ${jda.ping}ms")
    println("Invite link: ${jda.asBot().getInviteUrl()}")
}
