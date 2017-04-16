package com.serebit.autotitan

import com.google.common.reflect.ClassPath
import com.google.gson.Gson
import com.serebit.autotitan.annotations.CommandFunction
import com.serebit.autotitan.annotations.ListenerFunction
import com.serebit.autotitan.config.Configuration
import com.serebit.autotitan.data.Command
import com.serebit.autotitan.data.Listener
import com.serebit.autotitan.listeners.*
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.File
import java.util.*

fun main(args: Array<String>) {
  val useExistingSettings = !(args.contains("-r") || args.contains("--reset"))
  val configFile = File(Singleton.getParentDirectory().parent + "/data/config.json")
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
  jda.addEventListener(
      MessageListener(
          config.prefix,
          loadCommands(extensions),
          loadListeners(extensions, MessageListener.validEventTypes)
      ),
      JdaListener(loadListeners(extensions, JdaListener.validEventTypes)),
      UserListener(loadListeners(extensions, UserListener.validEventTypes)),
      TextChannelListener(loadListeners(extensions, TextChannelListener.validEventTypes)),
      VoiceChannelListener(loadListeners(extensions, VoiceChannelListener.validEventTypes))
  )
  println()
  println("Username:    ${jda.selfUser.name}")
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

fun loadCommands(classes: MutableSet<Class<*>>): MutableList<Command> {
  val commands = mutableListOf<Command>()
  classes.map { extension ->
    extension.methods
        .filter { it.isAnnotationPresent(CommandFunction::class.java) }
        .filter { it.parameterTypes[0] == MessageReceivedEvent::class.java }
        .forEach {
          commands.add(Command(extension.newInstance(), it, it.getAnnotation(CommandFunction::class.java)))
        }
  }
  return commands
}

fun loadListeners(classes: MutableSet<Class<*>>, validTypes: MutableSet<Class<out Event>>): MutableList<Listener> {
  val listeners = mutableListOf<Listener>()
  classes.map { extension ->
    extension.methods
        .filter { it.isAnnotationPresent(ListenerFunction::class.java) }
        .filter { it.parameterCount == 1 }
        .filter { it.parameterTypes[0] in validTypes }
        .forEach {
          listeners.add(Listener(extension.newInstance(), it, it.getAnnotation(ListenerFunction::class.java)))
        }
  }
  return listeners
}

object Singleton {
  fun getParentDirectory(): File = File(this::class.java.protectionDomain.codeSource.location.toURI())
}