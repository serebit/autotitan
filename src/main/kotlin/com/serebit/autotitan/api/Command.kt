package com.serebit.autotitan.api

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.config
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import com.serebit.autotitan.api.meta.annotations.Command as CommandAnnotation

class Command(
    private val function: KFunction<Unit>,
    private val instance: Any,
    val name: String,
    description: String,
    private val access: Access,
    private val locale: Locale,
    private val splitLastParameter: Boolean = true,
    private val isHidden: Boolean,
    private val memberPermissions: List<Permission> = emptyList()
) {
    private val parameterTypes: List<KClass<out Any>> = function.valueParameters.map { it.type.jvmErasure }.drop(1)
    val isNotHidden get() = !isHidden
    val summary = "`$name ${parameterTypes.joinToString(" ") { "<${it.simpleName}>" }}`"
    val helpField = MessageEmbed.Field(summary, description, false)

    operator fun invoke(evt: MessageReceivedEvent, parameters: List<Any>): Any? =
        function.call(instance, evt, *parameters.toTypedArray())

    fun looselyMatches(rawMessageContent: String): Boolean = rawMessageContent.split(" ")[0] == config.prefix + name

    fun parseTokensOrNull(evt: MessageReceivedEvent): List<Any>? {
        val tokens = tokenizeMessage(evt.message.contentRaw)
        if (evt.author.isBot) return null
        if (tokens[0] != config.prefix + name) return null
        if (parameterTypes.size != tokens.size - 1) return null
        if (evt.author.idLong in config.blackList) return null
        if (evt.guild != null && !evt.member.hasPermission(memberPermissions.toMutableList())) return null

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
        return if (splitLastParameter) {
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
        type: KClass<out Any>,
        string: String
    ): Any? = when (type) {
        String::class -> string
        Int::class -> string.toIntOrNull()
        Long::class -> string.toLongOrNull()
        Double::class -> string.toDoubleOrNull()
        Float::class -> string.toFloatOrNull()
        Short::class -> string.toShortOrNull()
        Byte::class -> string.toByteOrNull()
        Boolean::class -> {
            if (string == "true" || string == "false") string.toBoolean() else null
        }
        Char::class -> if (string.length == 1) string[0] else null
        User::class -> {
            evt.jda.getUserById(
                string
                    .removeSurrounding("<@", ">")
                    .removePrefix("!")
            )
        }
        Member::class -> {
            evt.guild.getMemberById(
                string
                    .removeSurrounding("<@", ">")
                    .removePrefix("!")
            )
        }
        Channel::class -> {
            evt.guild.getTextChannelById(
                string.removeSurrounding("<#", ">")
            )
        }
        else -> null
    }
}