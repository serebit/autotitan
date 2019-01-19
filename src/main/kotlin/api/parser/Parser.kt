package com.serebit.autotitan.api.parser

import com.serebit.autotitan.api.parameters.Emote
import com.serebit.autotitan.api.parameters.LongString
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

internal object Parser {
    fun tokenize(message: String, signature: Regex): List<String>? =
        signature.find(message)?.groups?.mapNotNull { it?.value }?.drop(1)

    fun parseTokens(evt: MessageReceivedEvent, tokens: List<String>, tokenTypes: List<TokenType>): List<Any>? =
        if (tokens.isNotEmpty()) castTokens(evt, tokenTypes, tokens.takeLast(tokenTypes.size)) else null

    private fun castTokens(evt: MessageReceivedEvent, types: List<TokenType>, tokens: List<String>): List<Any>? {
        val castedTokens = types.zip(tokens).map { (type, token) ->
            when (type) {
                is TokenType.Number -> castNumeral(type, token)
                is TokenType.Jda -> castJdaMentionable(evt, type, token)
                is TokenType.Other -> castOther(evt, type, token)
            }
        }
        return if (null in castedTokens) null else castedTokens.requireNoNulls()
    }

    private fun castJdaMentionable(evt: MessageReceivedEvent, type: TokenType.Jda, token: String): IMentionable? =
        when (type) {
            TokenType.Jda.UserToken -> evt.jda.getUserByMention(token)
            TokenType.Jda.MemberToken -> evt.guild.getMemberByMention(token)
            TokenType.Jda.TextChannelToken -> evt.guild.getTextChannelByMention(token)
            TokenType.Jda.RoleToken -> evt.guild.getRoleByMention(token)
        }

    private fun castNumeral(type: TokenType.Number, token: String): Number? = when (type) {
        TokenType.Number.ByteToken -> token.toByteOrNull()
        TokenType.Number.ShortToken -> token.toShortOrNull()
        TokenType.Number.IntToken -> token.toIntOrNull()
        TokenType.Number.LongToken -> token.toLongOrNull()
        TokenType.Number.BigIntToken -> token.toBigIntegerOrNull()
        TokenType.Number.DoubleToken -> token.toDoubleOrNull()
        TokenType.Number.FloatToken -> token.toFloatOrNull()
        TokenType.Number.BigDecimalToken -> token.toBigDecimalOrNull()
    }

    private fun castOther(evt: MessageReceivedEvent, type: TokenType.Other, token: String): Any? = when (type) {
        TokenType.Other.StringToken -> if (token.any { it.isWhitespace() }) null else token
        TokenType.Other.LongStringToken -> LongString(token)
        TokenType.Other.CharToken -> token.singleOrNull()
        TokenType.Other.BooleanToken -> token.toBooleanOrNull()
        TokenType.Other.EmoteToken -> Emote.from(token, evt.guild)
    }

    private fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

    private fun Guild.getMemberByMention(mention: String): Member? =
        getMemberById(mention.removeSurrounding("<@", ">").removePrefix("!"))

    private fun JDA.getUserByMention(mention: String): User? =
        retrieveUserById(mention.removeSurrounding("<@", ">").removePrefix("!")).complete()

    private fun Guild.getTextChannelByMention(mention: String): TextChannel? =
        getTextChannelById(mention.removeSurrounding("<#", ">"))

    private fun Guild.getRoleByMention(mention: String): Role? =
        getRoleById(mention.removeSurrounding("<@&", ">"))
}
