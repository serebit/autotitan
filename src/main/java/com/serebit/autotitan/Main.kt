package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.google.gson.Gson
import com.serebit.autotitan.annotations.ListenerFunction
import com.serebit.autotitan.config.Configuration
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.GuildCommand
import com.serebit.autotitan.data.Listener
import com.serebit.autotitan.listeners.*
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import java.io.File
import java.util.*

fun main(args: Array<String>) {
  val useExistingSettings = !(args.contains("-r") || args.contains("--reset"))
  val configFile = File("${Singleton.parentDirectory.parent}/data/config.json")
  val config: Configuration
  if (useExistingSettings && configFile.exists()) {
    config = Gson().fromJson(configFile.readText(), Configuration::class.java)
  } else {
    config = Configuration(
        getNewToken(),
        getNewPrefix()
    )
    configFile.parentFile.mkdirs()
    configFile.writeText(Gson().toJson(config))
  }
  val jda = JDABuilder(AccountType.BOT)
      .setToken(config.token)
      .buildBlocking()
  val extensions = getExtensions()
  val listeners = loadListeners(extensions)
  Command.prefix = config.prefix
  GuildCommand.prefix = config.prefix
  jda.addEventListener(
      MessageListener(
          loadCommands(extensions),
          listeners
      ),
      GuildMessageListener(
          loadGuildCommands(extensions),
          listeners
      ),
      JdaListener(listeners),
      UserListener(listeners),
      TextChannelListener(listeners),
      VoiceChannelListener(listeners)
  )
  println()
  println("Username:    ${jda.selfUser.name}")
  println("Ping:        ${jda.ping}ms")
  println("Invite link: ${jda.asBot().getInviteUrl()}")
}

fun getNewToken(): String {
  print("Enter new token:\n>")
  return Scanner(System.`in`).nextLine()
}

fun getNewPrefix(): String {
  print("Enter new prefix:\n>")
  return Scanner(System.`in`).nextLine()
}

fun getExtensions(): MutableSet<Class<*>> {
  val cp = ClassPath.from(Thread.currentThread().contextClassLoader)
  return cp.getTopLevelClassesRecursive("com.serebit.autotitan.extensions")
      .map { it.load() }
      .toMutableSet()
}

fun loadCommands(classes: MutableSet<Class<*>>): MutableSet<Command> {
  val commands = mutableSetOf<Command>()
  classes.map { extension ->
    val instance = extension.newInstance()
    extension.methods
        .filter { Command.isValidCommand(it) }
        .forEach { commands.add(Command(instance, it)) }
  }
  return commands
}

fun loadGuildCommands(classes: MutableSet<Class<*>>): MutableSet<GuildCommand> {
  val commands = mutableSetOf<GuildCommand>()
  classes.map { extension ->
    val instance = extension.newInstance()
    extension.methods
        .filter { GuildCommand.isValidCommand(it) }
        .forEach { commands.add(GuildCommand(instance, it)) }
  }
  return commands
}

fun loadListeners(classes: MutableSet<Class<*>>): MutableSet<Listener> {
  val listeners = mutableSetOf<Listener>()
  classes.map { extension ->
    extension.methods
        .filter { it.isAnnotationPresent(ListenerFunction::class.java) }
        .filter { it.parameterCount == 1 }
        .forEach {
          listeners.add(Listener(extension.newInstance(), it, it.getAnnotation(ListenerFunction::class.java)))
        }
  }
  return listeners
}

object Singleton {
  val parentDirectory = File(this::class.java.protectionDomain.codeSource.location.toURI())
}