package com.serebit.autotitan.api.parser

import com.serebit.autotitan.api.parameters.Emote
import com.serebit.extensions.jda.getMemberByMention
import com.serebit.extensions.jda.getTextChannelByMention
import com.serebit.extensions.jda.getUserByMention
import com.serebit.extensions.toBooleanOrNull
import net.dv8tion.jda.core.entities.IMentionable
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
        TokenType.OtherToken.StringToken -> token
        TokenType.OtherToken.CharToken -> token.singleOrNull()
        TokenType.OtherToken.BooleanToken -> token.toBooleanOrNull()
        TokenType.OtherToken.EmoteToken -> Emote.from(token, evt.jda)
    }
}
