package com.serebit.autotitan.api.parser

import com.serebit.autotitan.config

data class Token(val type: Tokens, val value: String)

enum class Tokens(val regex: Regex) {
    INVOCATION("\\s${Regex.escape(config.prefix)}\\w+\\s".toRegex()),
    INTEGER("\\s\\d+\\s".toRegex()),
    FLOAT("\\s-?\\d+\\.\\d+\\s".toRegex()),
    STRING("\\s\".+?\"\\s".toRegex()),
    USER("\\s<@!?\\d+?>\\s".toRegex()),
    MEMBER("\\s<@!?\\d+?>\\s".toRegex()),
    CHANNEL("\\s<#\\d+?>\\s".toRegex());
}