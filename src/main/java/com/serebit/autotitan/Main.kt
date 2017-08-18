package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.serebit.autotitan.api.annotations.ExtensionClass
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
            loadCommands(extensions),
            loadListeners(extensions)
    ))
    println()
    println("Username:    ${jda.selfUser.name}")
    println("Ping:        ${jda.ping}ms")
    println("Invite link: ${jda.asBot().getInviteUrl()}")
}

private fun loadExtensions(): Set<Class<*>> {
    val cp = ClassPath.from(Thread.currentThread().contextClassLoader)
    return cp.getTopLevelClassesRecursive("com.serebit.autotitan.extensions")
            .map { it.load() }
            .toSet()
}

private fun loadCommands(classes: Set<Class<*>>): Set<Command> {
    val commands = mutableSetOf<Command>()
    classes.filter { it.isAnnotationPresent(ExtensionClass::class.java) }.forEach { clazz ->
        val instance = Extension.generate(clazz)
        if (instance != null) {
            commands.addAll(clazz.methods.mapNotNull { Command.generate(instance, it) })
        }
    }
    return commands
}

private fun loadListeners(classes: Set<Class<*>>): Set<Listener> {
    val listeners = mutableSetOf<Listener>()
    classes.forEach { clazz ->
        val instance = Extension.generate(clazz)
        if (instance != null) {
            listeners.addAll(clazz.methods.mapNotNull { Listener.generate(instance, it) })
        }
    }
    return listeners
}
