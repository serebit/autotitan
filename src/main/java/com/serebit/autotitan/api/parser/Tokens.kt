package com.serebit.autotitan.api.parser

import com.serebit.autotitan.config

data class Token(val type: Tokens, val value: String)

enum class Tokens(val regex: Regex) {
    INVOCATION("${Regex.escape(config.prefix)}\\w+".toRegex()),
    INTEGER("\\d+".toRegex()),
    FLOAT("-?\\d+\\.\\d+".toRegex()),
    STRING("\".+?\"".toRegex(RegexOption.DOT_MATCHES_ALL)),
    USER("<@!?\\d+?>".toRegex()),
    MEMBER("<@!?\\d+?>".toRegex()),
    CHANNEL("<#\\d+?>".toRegex());
}