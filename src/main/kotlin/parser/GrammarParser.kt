package org.example.parser

import org.example.data.CFG
import org.example.data.Symbol
import org.example.data.TokenType

class GrammarParser {

    private val terminals = mutableSetOf<Char>()

    companion object {
        const val NT_REGEX = "^([A-Z][0-9]?|\\[[A-Za-z]+[0-9]*\\])\$"
        const val T_REGEX = "^[a-z]\$"
    }

    fun parse(grammar: String): CFG {
        val ruleMap = mutableMapOf<String, MutableList<List<Symbol>>>()

        var startSymbol = ""

        val lines = grammar.lines().filter { it.trim().isNotEmpty() }

        for (line in lines) {
            val (lhs, rhs) = parseRule(line)
            if (startSymbol.isEmpty()) {
                startSymbol = lhs
            }
            ruleMap.getOrPut(lhs) { mutableListOf() }.add(rhs)
        }

        return CFG(
            grammar = ruleMap,
            startSymbol = startSymbol,
            terminals = terminals
        )
    }

    private fun parseRule(line: String): Pair<String, List<Symbol>> {

        val parts = line.split("->", limit = 2).map { it.trim() }

        if (parts.size != 2) {
            throw IllegalArgumentException("Неверный формат правила (отсутствует '->'): '$line'")
        }

        val lhs = parseNT(parts[0]).value

        val rhs = parseRhs(parts[1])

        return Pair(lhs, rhs)
    }

    private fun parseRhs(rhs: String): List<Symbol> {

        val symbols = mutableListOf<Symbol>()
        var index = 0
        val length = rhs.length

        while (index < length) {
            when {

                rhs[index].isWhitespace() -> {
                    index++
                }

                rhs[index] == '[' -> {
                    val endBracket = rhs.indexOf(']', index)
                    if (endBracket == -1) {
                        throw IllegalArgumentException("Отсутствует закрывающая ']' для нетерминала в правой части: '$rhs'")
                    }
                    val nt = rhs.substring(index, endBracket + 1)
                    symbols.add(parseNT(nt))
                    index = endBracket + 1
                }

                rhs[index].isUpperCase() -> {
                    val start = index
                    index++
                    while (index < length && (rhs[index].isLetter())) {
                        index++
                    }
                    while (index < length && rhs[index].isDigit()) {
                        index++
                    }
                    val nt = rhs.substring(start, index)
                    symbols.add(parseNT(nt))
                }

                else -> {
                    val t = parseT(rhs, index)
                    symbols.add(t)
                    index += 1
                }
            }
        }

        return symbols
    }

    private fun parseNT(part: String): Symbol {
        val ntRegex = Regex(NT_REGEX)
        if (!ntRegex.matches(part)) {
            throw IllegalArgumentException("Неверный нетерминал: '$part'")
        }
        return Symbol(TokenType.NON_TERMINAL, part)
    }

    private fun parseT(rhs: String, index: Int): Symbol {
        val terminalChar = rhs[index]
        // if (terminalChar == 'ε') {
        //     return Symbol(TokenType.EPSILON, "ε")
        // }

        // val tRegex = Regex(T_REGEX)
        //
        // if (!tRegex.matches(terminalChar.toString())) {
        //     throw IllegalArgumentException("Неверный терминал: '$terminalChar' в правой части: '$rhs'")
        // }
        terminals.add(terminalChar)
        return Symbol(TokenType.TERMINAL, terminalChar.toString())
    }
}