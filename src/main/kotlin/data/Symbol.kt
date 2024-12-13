package org.example.data

enum class TokenType {
    NON_TERMINAL,
    TERMINAL,
}

data class Symbol(val type: TokenType, val value: String) {
    override fun toString(): String {
        return value
    }
}