package com.serebit.autotitan.data

import com.serebit.autotitan.api.Access
import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.config
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.lang.reflect.Method

class Command private constructor(private val instance: Any, internal val method: Method, info: CommandFunction) {
    private val parameterTypes: List<Class<*>> = method.parameterTypes.drop(1)
    val name = if (info.name.isNotEmpty()) {
        info.name
    } else {
        method.name
    }.toLowerCase()
    private val description = if (info.description.isNotBlank()) info.description else "No description provided."
    private val access = info.access
    private val locale = info.locale
    private val delimitFinalParameter = info.delimitFinalParameter
    private val hidden = info.hidden
    private val permissions = info.permissions
    val helpMessage = if (hidden) "" else "`$name` - $description"

    operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>): Any? =
            method.invoke(instance, evt, *parameters.toTypedArray())

    fun looselyMatches(rawMessageContent: String): Boolean {
        return rawMessageContent.split(" ")[0] == config.prefix + name
    }

    fun castParametersOrNull(evt: MessageReceivedEvent): List<Any>? {
        if (evt.message.rawContent.split(" ")[0] != config.prefix + name) return null
        if (parameterTypes.size != tokenizeMessage(evt.message.rawContent).size) return null
        if (evt.author.idLong in config.blackList) return null
        val correctLocale = when (locale) {
            Locale.ALL -> true
            Locale.GUILD -> evt.guild != null
            Locale.PRIVATE_CHANNEL -> evt.guild == null
        }
        val hasPermissions = if (evt.guild != null) evt.member.hasPermission(permissions.toMutableList()) else true
        val hasAccess = when (access) {
            Access.ALL -> true
            Access.GUILD_OWNER -> evt.member == evt.guild?.owner
            Access.BOT_OWNER -> evt.author == evt.jda.asBot().applicationInfo.complete().owner
            Access.RANK_ABOVE -> TODO("Not yet implemented.")
            Access.RANK_SAME -> TODO("Not yet implemented.")
            Access.RANK_BELOW -> TODO("Not yet implemented.")
        }

        return if (correctLocale && hasPermissions && hasAccess) {
            val castParameters = castParameters(evt)
            if (castParameters.any { it == null }) null else castParameters.filterNotNull()
        } else null
    }

    private fun tokenizeMessage(message: String): List<String> {
        val trimmedMessage = message.removePrefix(config.prefix + name).trim()
        val splitParameters = trimmedMessage.split(" ").filter(String::isNotBlank)
        return if (delimitFinalParameter) {
            splitParameters
        } else {
            mutableListOf(
                    *splitParameters.slice(0..(parameterTypes.size - 2)).toTypedArray(),
                    splitParameters.drop(parameterTypes.size - 1).joinToString(" ")
            )
        }.filter(String::isNotBlank)
    }

    private fun castParameters(evt: MessageReceivedEvent): List<Any?> {
        val strings = tokenizeMessage(evt.message.rawContent)
        return parameterTypes.zip(strings).map { (type, string) ->
            castParameter(evt, type, string)
        }.toList()
    }

    private fun castParameter(evt: MessageReceivedEvent, type: Class<*>, string: String): Any? = when (type) {
        Int::class.java -> string.toIntOrNull()
        Long::class.java -> string.toLongOrNull()
        Double::class.java -> string.toDoubleOrNull()
        Float::class.java -> string.toFloatOrNull()
        Short::class.java -> string.toShortOrNull()
        Byte::class.java -> string.toByteOrNull()
        Boolean::class.java -> {
            if (string == "true" || string == "false") string.toBoolean() else null
        }
        Char::class.java -> if (string.length == 1) string[0] else null
        User::class.java -> {
            evt.jda.getUserById(string
                    .removePrefix("<@")
                    .removePrefix("!")
                    .removeSuffix(">")
            )
        }
        Member::class.java -> {
            evt.guild.getMemberById(string
                    .removePrefix("<@")
                    .removePrefix("!")
                    .removeSuffix(">")
            )
        }
        Channel::class.java -> evt.guild.getTextChannelById(string)
        String::class.java -> string
        else -> null
    }

    companion object {
        private val validParameterTypes = setOf(
                Int::class.java,
                Long::class.java,
                Double::class.java,
                Float::class.java,
                User::class.java,
                Member::class.java,
                Channel::class.java,
                String::class.java
        )

        internal fun generate(instance: Any, method: Method): Command? {
            return if (isValid(method)) Command(
                    instance,
                    method,
                    method.getAnnotation(CommandFunction::class.java)
            ) else null
        }

        internal fun isValid(method: Method): Boolean {
            return if (method.parameterTypes.isNotEmpty()) {
                val hasAnnotation = method.isAnnotationPresent(CommandFunction::class.java)
                val hasEventParameter = method.parameterTypes[0] == MessageReceivedEvent::class.java
                val hasValidParameterTypes by lazy {
                    method.parameterTypes
                            .drop(1)
                            .all { it in validParameterTypes }
                }
                hasAnnotation && hasEventParameter && hasValidParameterTypes
            } else false
        }
    }
}