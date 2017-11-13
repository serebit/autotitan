package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.CommandFunction
import com.serebit.autotitan.config
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.lang.reflect.Method

class Command(
        private val instance: Any,
        internal val method: Method,
        info: CommandFunction
) {
    private val parameterTypes: List<Class<out Any>> = method.parameterTypes.drop(1).toList()
    val name = if (info.name.isNotEmpty()) {
        info.name
    } else {
        method.name
    }.toLowerCase()
    private val description = if (info.description.isNotBlank()) info.description else ""
    private val access = info.access
    private val locale = info.locale
    private val delimitFinalParameter = info.delimitFinalParameter
    private val hidden = info.hidden
    private val permissions = info.permissions
    val helpMessage = if (hidden) "" else "`$name`" + if (description.isNotEmpty()) "- $description" else ""

    operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>): Any? =
            method.invoke(instance, evt, *parameters.toTypedArray())

    fun looselyMatches(rawMessageContent: String): Boolean {
        return rawMessageContent.split(" ")[0] == config.prefix + name
    }

    fun parseTokensOrNull(evt: MessageReceivedEvent): List<Any>? {
        val tokens = tokenizeMessage(evt.message.rawContent)
        if (tokens[0] != config.prefix + name) return null
        if (parameterTypes.size != tokens.size - 1) return null
        if (evt.author.idLong in config.blackList) return null
        if (evt.guild != null && !evt.member.hasPermission(permissions.toMutableList())) return null

        val correctLocale = when (locale) {
            Locale.ALL -> true
            Locale.GUILD -> evt.guild != null
            Locale.PRIVATE_CHANNEL -> evt.guild == null
        }
        val hasAccess = when (access) {
            Access.ALL -> true
            Access.GUILD_OWNER -> evt.member == evt.guild?.owner
            Access.BOT_OWNER -> evt.author == evt.jda.asBot().applicationInfo.complete().owner
            Access.RANK_ABOVE -> TODO("Not yet implemented.")
            Access.RANK_SAME -> TODO("Not yet implemented.")
            Access.RANK_BELOW -> TODO("Not yet implemented.")
        }

        return if (correctLocale && hasAccess) {
            val parsedTokens = parseTokens(evt, tokens)
            if (parsedTokens.any { it == null }) null else parsedTokens.filterNotNull()
        } else null
    }

    private fun tokenizeMessage(message: String): List<String> {
        val splitParameters = message.split(" ").filter(String::isNotBlank)
        return if (delimitFinalParameter) {
            splitParameters
        } else {
            listOf(
                    *splitParameters.slice(0..(parameterTypes.size - 1)).toTypedArray(),
                    splitParameters.drop(parameterTypes.size).joinToString(" ")
            )
        }.filter(String::isNotBlank)
    }

    private fun parseTokens(evt: MessageReceivedEvent, tokens: List<String>): List<Any?> {
        return parameterTypes.zip(tokens.drop(1)).map { (type, string) ->
            castParameter(evt, type, string)
        }
    }

    private fun castParameter(
            evt: MessageReceivedEvent,
            type: Class<out Any>,
            string: String
    ): Any? = when (type) {
        String::class.java -> string
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
                    .removeSurrounding("<@", ">")
                    .removePrefix("!")
            )
        }
        Member::class.java -> {
            evt.guild.getMemberById(string
                    .removeSurrounding("<@", ">")
                    .removePrefix("!")
            )
        }
        Channel::class.java -> {
            evt.guild.getTextChannelById(
                    string.removeSurrounding("<#", ">")
            )
        }
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
            if (method.parameterTypes.isEmpty()) return false
            if (!method.isAnnotationPresent(CommandFunction::class.java)) return false
            if (method.parameterTypes[0] != MessageReceivedEvent::class.java) return false
            return method.parameterTypes
                    .drop(1)
                    .all { it in validParameterTypes }
        }
    }
}