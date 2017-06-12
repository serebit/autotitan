package com.serebit.autotitan.data

import com.serebit.autotitan.api.Access
import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.config
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.lang.reflect.Method

class Command(val instance: Any, val method: Method, info: CommandFunction) {
  val parameterTypes = method.parameterTypes.toMutableList()
  val name: String
  val description: String
  val access: Access
  val locale: Locale
  val delimitFinalParameter: Boolean
  val hidden: Boolean
  val permissions: MutableSet<Permission>

  init {
    parameterTypes.removeAt(0)
    name = when (info.name) {
      "" -> method.name.toLowerCase()
      else -> info.name
    }
    description = info.description
    access = info.access
    locale = info.locale
    delimitFinalParameter = info.delimitFinalParameter
    hidden = info.hidden
    permissions = info.permissions.toMutableSet()
    if (parameterTypes.any { it !in validParameterTypes })
      throw IllegalArgumentException("Invalid argument type passed to Command constructor.")
  }

  operator fun invoke(evt: MessageReceivedEvent) {
    method.invoke(instance, evt, *castParameters(evt).toTypedArray())
  }

  fun matches(evt: MessageReceivedEvent): Boolean {
    val correctInvocation = evt.message.rawContent.startsWith(config.prefix + name)
    val correctAccess = when (access) {
      Access.ALL -> true
      Access.GUILD_OWNER -> evt.member == evt.guild?.owner
      Access.BOT_OWNER -> evt.author == evt.jda.asBot().applicationInfo.complete().owner
      Access.RANK_ABOVE -> TODO("Not yet implemented.")
      Access.RANK_SAME -> TODO("Not yet implemented.")
      Access.RANK_BELOW -> TODO("Not yet implemented.")
    }
    val correctLocale = when (locale) {
      Locale.ALL -> true
      Locale.GUILD -> evt.guild != null
      Locale.PRIVATE_CHANNEL -> evt.guild == null
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
    return evt.message.rawContent.startsWith(config.prefix + name)
  }

  fun sendHelpMessage(evt: MessageReceivedEvent) {
    val parameterTypesString = parameterTypes.map(Class<*>::getSimpleName).joinToString(" ")
    val helpMessage = "```\n${config.prefix}$name $parameterTypesString\n\n$description```"
    evt.channel.sendMessage(helpMessage)
  }

  private fun getMessageParameters(message: String): MutableList<String> {
    val trimmedMessage = message.removePrefix(config.prefix + name).trim()
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
    @JvmStatic internal val validParameterTypes = mutableSetOf(
        Int::class.java,
        Long::class.java,
        Double::class.java,
        Float::class.java,
        User::class.java,
        Member::class.java,
        Channel::class.java,
        String::class.java
    )

    @JvmStatic internal fun isValid(method: Method): Boolean {
      return if (method.parameterTypes.isNotEmpty()) {
        val hasAnnotation = method.isAnnotationPresent(CommandFunction::class.java)
        val hasEventParameter = method.parameterTypes[0] == MessageReceivedEvent::class.java
        val parameterTypes = method.parameterTypes.toMutableList().apply { removeAt(0) }
        val hasValidParameterTypes = parameterTypes.all { it in validParameterTypes }
        hasAnnotation && hasEventParameter && hasValidParameterTypes
      } else false
    }
  }
}