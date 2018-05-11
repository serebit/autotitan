package com.serebit.autotitan.api.parser

import com.serebit.autotitan.api.parameters.Emote
import com.serebit.autotitan.api.parameters.LongString
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

sealed class TokenType(val name: String) {
    sealed class NumberToken(name: String) : TokenType(name) {
        object ByteToken : NumberToken("Byte")
        object ShortToken : NumberToken("Short")
        object IntToken : NumberToken("Int")
        object LongToken : NumberToken("Long")
        object FloatToken : NumberToken("Float")
        object DoubleToken : NumberToken("Double")

        companion object {
            fun from(type: KClass<out Any>): NumberToken? = when(type) {
                Byte::class -> ByteToken
                Short::class -> ShortToken
                Int::class -> IntToken
                Long::class -> LongToken
                Float::class -> FloatToken
                Double::class -> DoubleToken
                else -> null
            }
        }
    }

    sealed class JdaToken(name: String) : TokenType(name) {
        object UserToken : JdaToken("User")
        object MemberToken : JdaToken("Member")
        object ChannelToken : JdaToken("Channel")

        companion object {
            fun from(type: KClass<out Any>): JdaToken? = when(type) {
                User::class -> UserToken
                Member::class -> MemberToken
                Channel::class -> ChannelToken
                else -> null
            }
        }
    }

    sealed class OtherToken(name: String) : TokenType(name) {
        object EmoteToken : OtherToken("Emote")
        object StringToken : OtherToken("String")
        object LongStringToken : OtherToken("LongString")
        object BooleanToken : OtherToken("Boolean")
        object CharToken : OtherToken("Char")

        companion object {
            fun from(type: KClass<out Any>): OtherToken? = when(type) {
                Emote::class -> EmoteToken
                String::class -> StringToken
                LongString::class -> LongStringToken
                Boolean::class -> BooleanToken
                Char::class -> CharToken
                else -> null
            }
        }
    }

    companion object {
        fun from(type: KClass<out Any>): TokenType? = when {
            type.isSubclassOf(Number::class) -> NumberToken.from(type)
            type.isSubclassOf(IMentionable::class) -> JdaToken.from(type)
            else -> OtherToken.from(type)
        }
    }
}
