package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.config.Configuration
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Extension
import com.serebit.autotitan.data.Listener
import com.serebit.autotitan.listeners.EventListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder

const val version = "0.0.3"

fun main(args: Array<String>) {
    val jda = JDABuilder(AccountType.BOT).apply {
        setToken(Configuration.token)
    }.buildBlocking()

    val extensions = loadExtensions()
    jda.addEventListener(EventListener(
            loadCommands(extensions).toSet(),
            loadListeners(extensions).toSet()
    ))
    println()
    println("Username:    ${jda.selfUser.name}")
    println("Ping:        ${jda.ping}ms")
    println("Invite link: ${jda.asBot().getInviteUrl()}")
}

private fun loadExtensions() = ClassPath.from(Thread.currentThread().contextClassLoader)
        .getTopLevelClassesRecursive("com.serebit.autotitan.extensions")
        .map { it.load() }

private fun loadCommands(classes: List<Class<*>>): List<Command> {
    return classes.mapNotNull { clazz ->
        val instance = Extension.generate(clazz)
        if (instance != null) {
            clazz.methods.mapNotNull { Command.generate(instance, it) }
        } else null
    }.flatten()
}

private fun loadListeners(classes: List<Class<*>>): List<Listener> {
    return classes.mapNotNull { clazz ->
        val instance = Extension.generate(clazz)
        if (instance != null) {
            clazz.methods.mapNotNull { Listener.generate(instance, it) }
        } else null
    }.flatten()
}
