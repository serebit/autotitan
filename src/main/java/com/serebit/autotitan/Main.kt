package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.api.Command
import com.serebit.autotitan.api.Listener
import com.serebit.autotitan.api.generateModule
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA

const val NAME = "AutoTitan"
const val VERSION = "0.3.2"

fun main(args: Array<String>) {
    val jda = initJda()

    println()
    println("$NAME v$VERSION")
    println("Username:    ${jda.selfUser.name}")
    println("Ping:        ${jda.ping}ms")
    println("Invite link: ${jda.asBot().getInviteUrl()}")
}

private fun initJda(): JDA {
    val modules = ClassPath
            .from(Thread.currentThread().contextClassLoader)
            .getTopLevelClassesRecursive("com.serebit.autotitan.modules")
            .mapNotNull { generateModule(it.load()) }
    val commands = modules.map { instance ->
        instance::class.java.methods.mapNotNull { method -> Command.generate(instance, method) }
    }.flatten()
    val listeners = modules.map { instance ->
        instance::class.java.methods.mapNotNull { method -> Listener.generate(instance, method) }
    }.flatten()
    return jda(AccountType.BOT) {
        setToken(config.token)
        addEventListener(EventListener(commands, listeners))
    }
}