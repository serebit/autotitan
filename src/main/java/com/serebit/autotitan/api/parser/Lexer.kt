package com.serebit.autotitan.api.parser

fun tokenizeMessage(message: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var position = 0
    val numWhitespaces = message.count { it.isWhitespace() }
    (0..numWhitespaces).forEach {
        val whitespaceIndex = if (it == numWhitespaces) message.length
        else message.drop(position).indexOfFirst { it.isWhitespace() } + position
        val substring = message.substring(position until whitespaceIndex)
        val tokenType = TokenType.values().firstOrNull { substring.matches(it.regex) } ?: return@forEach
        tokens.add(Token(tokenType, substring))
        position = whitespaceIndex + 1
    }
    return tokens
}
