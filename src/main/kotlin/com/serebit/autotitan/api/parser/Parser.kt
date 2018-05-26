package com.serebit.autotitan.api.parser

import com.serebit.autotitan.api.parameters.Emote
import com.serebit.autotitan.api.parameters.LongString
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

internal object Parser {
    fun castToken(evt: MessageReceivedEvent, type: TokenType, token: String): Any? = when (type) {
        is TokenType.NumberToken -> castNumeral(type, token)
        is TokenType.JdaToken -> castJdaMentionable(evt, type, token)
        is TokenType.OtherToken -> castOther(evt, type, token)
    }

    private fun castJdaMentionable(evt: MessageReceivedEvent, type: TokenType.JdaToken, token: String): IMentionable? =
        when (type) {
            TokenType.JdaToken.UserToken -> evt.jda.getUserByMention(token)
            TokenType.JdaToken.MemberToken -> evt.guild.getMemberByMention(token)
            TokenType.JdaToken.ChannelToken -> evt.guild.getTextChannelByMention(token)
        }

    private fun castNumeral(type: TokenType.NumberToken, token: String): Number? = when (type) {
        TokenType.NumberToken.ByteToken -> token.toByteOrNull()
        TokenType.NumberToken.ShortToken -> token.toShortOrNull()
        TokenType.NumberToken.IntToken -> token.toIntOrNull()
        TokenType.NumberToken.LongToken -> token.toLongOrNull()
        TokenType.NumberToken.DoubleToken -> token.toDoubleOrNull()
        TokenType.NumberToken.FloatToken -> token.toFloatOrNull()
    }

    private fun castOther(evt: MessageReceivedEvent, type: TokenType.OtherToken, token: String): Any? = when (type) {
        TokenType.OtherToken.StringToken -> if (token.contains("\\s".toRegex())) null else token
        TokenType.OtherToken.LongStringToken -> LongString(token)
        TokenType.OtherToken.CharToken -> token.singleOrNull()
        TokenType.OtherToken.BooleanToken -> token.toBooleanOrNull()
        TokenType.OtherToken.EmoteToken -> Emote.from(token, evt.jda)
    }

    private fun String.toBooleanOrNull() = if (this == "true" || this == "false") toBoolean() else null

    private fun Guild.getMemberByMention(mention: String): Member? =
        getMemberById(mention.removeSurrounding("<@", ">").removePrefix("!"))

    private fun JDA.getUserByMention(mention: String): User? =
        retrieveUserById(mention.removeSurrounding("<@", ">").removePrefix("!")).complete()

    private fun Guild.getTextChannelByMention(mention: String): TextChannel? =
        getTextChannelById(mention.removeSurrounding("<#", ">"))
}
