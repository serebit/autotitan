package com.serebit.autotitan.data

import com.serebit.autotitan.Access
import com.serebit.autotitan.Locale
import com.serebit.autotitan.annotations.CommandFunction
import com.sun.javaws.exceptions.InvalidArgumentException
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.lang.reflect.Method

class Command(val instance: Any, val method: Method) {
  val parameterTypes: MutableList<Class<*>>
  val name: String
  val description: String
  val delimitFinalParameter: Boolean
  val access: Access
  val locale: Locale
  val permissions: MutableSet<Permission>

  init {
    val info = method.getAnnotation(CommandFunction::class.java)
    val parameterList = method.parameterTypes.toMutableList()
    parameterList.removeAt(0) // Remove event parameter
    parameterTypes = parameterList
    name = when (info.name) {
      "" -> method.name.toLowerCase()
      else -> info.name
    }
    description = info.description
    delimitFinalParameter = info.delimitFinalParameter
    access = info.access
    locale = info.locale
    permissions = info.permissions.toMutableSet()
    if (parameterList.any { it !in validParameterTypes })
      throw InvalidArgumentException(arrayOf("Invalid argument type passed to Command constructor."))
  }

  operator fun invoke(evt: MessageReceivedEvent) {
    method.invoke(instance, evt, *castParameters(evt).toTypedArray())
  }

  fun matches(evt: MessageReceivedEvent): Boolean {
    val correctInvocation = evt.message.rawContent.startsWith(prefix + name)
    // TODO Fix logic here so it doesn't block other threads.
    val correctAccess = when (access) {
      Access.ALL -> true
      Access.GUILD_OWNER -> evt.member == evt.guild?.owner
      Access.BOT_OWNER -> evt.author == evt.jda.asBot().applicationInfo.complete().owner
    }
    val correctLocale = when (locale) {
      Locale.ALL -> true
      Locale.GUILD -> evt.guild != null
      Locale.PRIVATE -> evt.guild == null
    }
    val hasPermissions = when (evt.guild != null) {
      true -> evt.member.hasPermission(permissions)
      else -> true
    }
    if (correctInvocation && correctAccess && correctLocale && hasPermissions) {
      val strings = getMessageParameters(evt.message.rawContent)
      if (parameterTypes.size != strings.size) return false
      return parameterTypes.zip(strings).all {
        (type, string) ->
        validateParameter(evt, type, string)
      }
    } else {
      return false
    }
  }

  fun roughlyMatches(evt: MessageReceivedEvent): Boolean {
    return evt.message.rawContent.startsWith(prefix + name)
  }

  fun sendHelpMessage(evt: MessageReceivedEvent) {
    val parameterTypesString = parameterTypes.map(Class<*>::getSimpleName).joinToString(" ")
    val helpMessage = "```\n$prefix$name $parameterTypesString\n\n$description```"
    evt.channel.sendMessage(helpMessage)
  }

  private fun getMessageParameters(message: String): MutableList<String> {
    val trimmedMessage = message.removePrefix(prefix + name).trim()
    val parameterCount = parameterTypes.size
    val splitParameters = trimmedMessage.split(" ").filter(String::isNotBlank).toMutableList()
    if (delimitFinalParameter) {
      return splitParameters
    } else {
      val parameters = mutableListOf<String>()
      (0..parameterCount - 2).forEach {
        parameters.add(splitParameters[it])
        splitParameters.removeAt(0)
      }
      if (splitParameters.size > 0) {
        parameters.add(splitParameters.joinToString(" "))
      }
      return parameters
    }
  }

  private fun validateParameter(evt: MessageReceivedEvent, type: Class<*>, string: String): Boolean {
    return when (type) {
      Int::class.java -> string.toIntOrNull() != null
      Long::class.java -> string.toLongOrNull() != null
      Double::class.java -> string.toDoubleOrNull() != null
      Float::class.java -> string.toFloatOrNull() != null
      User::class.java -> {
        val trimmed = evt.jda.getUserById(string.removePrefix("<@").removePrefix("!").removeSuffix(">"))
        trimmed != null
      }
      Member::class.java -> {
        val trimmed = evt.guild.getMemberById(string.removePrefix("<@").removePrefix("!").removeSuffix(">"))
        trimmed != null
      }
      Channel::class.java -> evt.guild.getTextChannelById(string) != null
      String::class.java -> true
      else -> {
        println(type.canonicalName + " is not a valid parameter type.")
        false
      }
    }
  }

  private fun castParameters(evt: MessageReceivedEvent): MutableList<Any> {
    val strings = getMessageParameters(evt.message.rawContent)
    return parameterTypes.zip(strings).map {
      (type, string) ->
      castParameter(evt, type, string)
    }.toMutableList()
  }

  private fun castParameter(evt: MessageReceivedEvent, type: Class<*>, string: String): Any {
    return when (type) {
      Int::class.java -> string.toInt()
      Long::class.java -> string.toLong()
      Double::class.java -> string.toDouble()
      Float::class.java -> string.toFloat()
      User::class.java -> {
        val trimmed = evt.jda.getUserById(string.removePrefix("<@").removePrefix("!").removeSuffix(">"))
        trimmed
      }
      Member::class.java -> {
        val trimmed = evt.guild.getMemberById(string.removePrefix("<@").removePrefix("!").removeSuffix(">"))
        trimmed
      }
      Channel::class.java -> evt.guild.getTextChannelById(string)
      else -> string
    }
  }

  companion object {
    @JvmStatic var prefix: String = "!"
      set(value) {
        field = when {
          value.length < 4 -> value
          else -> value.substring(0, 2)
        }
      }
    @JvmStatic val validParameterTypes = mutableSetOf(
        Int::class.java,
        Long::class.java,
        Double::class.java,
        Float::class.java,
        User::class.java,
        Member::class.java,
        Channel::class.java,
        String::class.java
    )

    @JvmStatic fun isValidCommand(method: Method): Boolean {
      val methodHasAnnotation = method.isAnnotationPresent(CommandFunction::class.java)
      val methodHasParameters = method.parameterCount > 0
      return run {
        methodHasAnnotation && methodHasParameters && method.parameterTypes[0] == MessageReceivedEvent::class.java
      }
    }
  }
}