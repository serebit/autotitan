package com.serebit.autotitan.api.parser

fun tokenizeMessage(message: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var position = 0
    var size = 1
    val formattedMessage = " $message "
    while (position + size <= formattedMessage.length) {
        val substring = formattedMessage.substring(position, position + size)
        if (Tokens.values().none { substring.matches(it.regex) }) {
            size++
            continue
        }
        val tokenType = Tokens.values().first { substring.matches(it.regex) }
        tokens.add(Token(tokenType, substring.substring(1, substring.length - 1)))
        position += size - 1
        size = 1
    }
    return tokens
}
