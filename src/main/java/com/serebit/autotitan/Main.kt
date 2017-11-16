package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.api.Command
import com.serebit.autotitan.api.Listener
import com.serebit.autotitan.api.generateModule
import com.serebit.autotitan.listeners.EventListener
import com.serebit.extensions.jda.jda
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.events.Event

const val NAME = "Spyglass"
const val VERSION = "0.3.2"
private val modules = ClassPath
        .from(Thread.currentThread().contextClassLoader)
        .getTopLevelClassesRecursive("com.serebit.autotitan.modules")
        .mapNotNull { generateModule(it.load()) }
private val commands
    get() = modules.map { instance ->
        instance::class.java.methods.mapNotNull { method -> Command.generate(instance, method) }
    }.flatten()
private val listeners
    get() = modules.map { instance ->
        instance::class.java.methods.mapNotNull { method -> Listener.generate(instance, method) }
    }.flatten()

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

fun resetJda(evt: Event) {
    evt.run {
        jda.registeredListeners.forEach { jda.removeEventListener(it) }
        jda.addEventListener(EventListener(commands, listeners))
    }
}