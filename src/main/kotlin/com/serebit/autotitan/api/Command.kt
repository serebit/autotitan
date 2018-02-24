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

internal class Command(
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

    internal fun looselyMatches(rawMessageContent: String): Boolean {
        return rawMessageContent.split(" ")[0] == config.prefix + name
    }

    internal fun parseTokensOrNull(evt: MessageReceivedEvent): List<Any>? {
        if (!evt.isInvalidCommandInvocation) return null
        val tokens = tokenizeMessage(evt.message.contentRaw)
        return when {
            tokens[0] != config.prefix + name -> null
            parameterTypes.size != tokens.size - 1 -> null
            else -> {
                val parsedTokens = parseTokens(evt, tokens)
                if (parsedTokens.any { it == null }) null else parsedTokens.filterNotNull()
            }
        }
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

    @Suppress("ComplexMethod")
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
        Boolean::class -> string.toBooleanOrNull()
        Char::class -> string.toCharOrNull()
        User::class -> evt.jda.getUserById(
            string
                .removeSurrounding("<@", ">")
                .removePrefix("!")
        )
        Member::class -> evt.guild.getMemberById(
            string
                .removeSurrounding("<@", ">")
                .removePrefix("!")
        )
        Channel::class -> evt.guild.getTextChannelById(
            string.removeSurrounding("<#", ">")
        )
        else -> null
    }

//    operator fun Role.compareTo(other: Role) = guild.roles.indexOf(this).compareTo(guild.roles.indexOf(other))

    private val MessageReceivedEvent.isValidCommandInvocation: Boolean
        get() {
            val hasAccess = when (access) {
                Access.ALL -> true
                Access.GUILD_OWNER -> member == guild?.owner
                Access.BOT_OWNER -> author == jda.asBot().applicationInfo.complete().owner
                Access.RANK_ABOVE -> member.roles[0] > guild.selfMember.roles[0]
                Access.RANK_SAME -> member.roles[0].compareTo(guild.selfMember.roles[0]) == 0
                Access.RANK_BELOW -> member.roles[0] < guild.selfMember.roles[0]
            }

            val correctLocale = when (locale) {
                Locale.ALL -> true
                Locale.GUILD -> guild != null
                Locale.PRIVATE_CHANNEL -> guild == null
            }

            return when {
                author.isBot -> false
                author.idLong in config.blackList -> false
                guild != null && !member.hasPermission(memberPermissions.toMutableList()) -> false
                else -> hasAccess && correctLocale
            }
        }

    private val MessageReceivedEvent.isInvalidCommandInvocation: Boolean get() = !isValidCommandInvocation

    private fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

    private fun String.toCharOrNull() = if (length == 1) this[0] else null
}
