package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.google.gson.Gson
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ListenerFunction
import com.serebit.autotitan.config.Configuration
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Listener
import com.serebit.autotitan.listeners.EventListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import java.io.File
import java.util.*

const val version = "0.0.2"
val config = Configuration()

fun main(args: Array<String>) {
  val jda = JDABuilder(AccountType.BOT)
      .setToken(config.token)
      .buildBlocking()
  val extensions = loadExtensions()
  jda.addEventListener(
      EventListener(
          loadCommands(extensions),
          loadListeners(extensions)
      )
  )
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
  classes.map { extension ->
    val commandMethods = extension.methods.filter { Command.isValid(it) }
    if (commandMethods.isNotEmpty()) {
      val instance = extension.newInstance()
      commands.addAll(commandMethods.map {
        Command(
            instance,
            it,
            it.getAnnotation(CommandFunction::class.java)
        )
      })
    }
  }
  return commands
}

private fun loadListeners(classes: Set<Class<*>>): Set<Listener> {
  val listeners = mutableSetOf<Listener>()
  classes.map { extension ->
    val extensionListeners = extension.methods.filter { Listener.isValid(it) }
    if (extensionListeners.isNotEmpty()) {
      val instance = extension.newInstance()
      listeners.addAll(extensionListeners.map {
        Listener(
            instance,
            it,
            it.getAnnotation(ListenerFunction::class.java)
        )
      })
    }
  }
  return listeners
}
