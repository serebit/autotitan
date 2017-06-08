package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.google.gson.Gson
import com.serebit.autotitan.api.annotations.ListenerFunction
import com.serebit.autotitan.config.Configuration
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Listener
import com.serebit.autotitan.listeners.EventListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.Event
import java.io.File
import java.util.*

val config: Configuration = loadOrCreateConfig()

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

private fun prompt(prompt: String): String {
  print("$prompt\n> ")
  return Scanner(System.`in`).nextLine()
}

fun loadExtensions(): MutableSet<Class<*>> {
  val cp = ClassPath.from(Thread.currentThread().contextClassLoader)
  return cp.getTopLevelClassesRecursive("com.serebit.autotitan.extensions")
      .map { it.load() }
      .toMutableSet()
}

fun loadCommands(classes: MutableSet<Class<*>>): MutableSet<Command> {
  val commands = mutableSetOf<Command>()
  classes.map { extension ->
    val commandMethods = extension.methods
        .filter { Command.isValidCommand(it) }
    if (commandMethods.isNotEmpty()) {
      val instance = extension.newInstance()
      commandMethods.forEach { commands.add(Command(instance, it)) }
    }
  }
  return commands
}

fun loadListeners(classes: MutableSet<Class<*>>): MutableSet<Listener> {
  val listeners = mutableSetOf<Listener>()
  classes.map { extension ->
    extension.methods
        .filter { it.isAnnotationPresent(ListenerFunction::class.java) }
        .filter { it.parameterCount == 1 }
        .filter { Event::class.java.isAssignableFrom(it.parameterTypes[0]) }
        .forEach {
          listeners.add(Listener(
              extension.newInstance(),
              it,
              it.getAnnotation(ListenerFunction::class.java)
          ))
        }
  }
  return listeners
}

private fun loadOrCreateConfig(): Configuration {
  val parentFolder = File(
      Configuration::class.java.protectionDomain.codeSource.location.toURI()
  )
  val configFile = File("$parentFolder/data/config.json")
  if (configFile.exists()) {
    return Gson().fromJson(configFile.readText(), Configuration::class.java)
  } else {
    val config = Configuration(
        prompt("Enter new token: "),
        prompt("Enter new command prefix: ")
    )
    configFile.parentFile.mkdirs()
    configFile.writeText(Gson().toJson(config))
    return config
  }
}
