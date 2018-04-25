package com.serebit.autotitan.api.parser

internal fun tokenizeMessage(message: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var position = 0
    val numWhitespaces = message.count { it.isWhitespace() }
    for (i in 0..numWhitespaces) {
        val whitespaceIndex = if (i == numWhitespaces) message.length
        else message.drop(position).indexOfFirst { it.isWhitespace() } + position
        val substring = message.substring(position until whitespaceIndex)
        TokenType.values().firstOrNull { substring.matches(it.regex) }?.let { tokenType ->
            tokens.add(Token(tokenType, substring))
            position = whitespaceIndex + 1
        }
    }
    return tokens
}
