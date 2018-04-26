package com.serebit.autotitan.api.parser

internal fun tokenizeMessage(message: String): List<Token> {
    val tokens = mutableListOf<Token>()
    val numWhitespaces = message.count { it.isWhitespace() }
    (0..numWhitespaces).fold(0) { position, i ->
        val whitespaceIndex = if (i == numWhitespaces) message.length
        else message.drop(position).indexOfFirst { it.isWhitespace() } + position
        val substring = message.substring(position until whitespaceIndex)
        TokenType.values().find { substring.matches(it.regex) }?.let { tokenType ->
            tokens.add(Token(tokenType, substring))
        }
        whitespaceIndex + 1
    }
    return tokens
}
