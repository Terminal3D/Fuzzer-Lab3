package org.example.parser

import org.example.data.CFG
import org.example.data.TokenType

class CYKParser(private val cfg: CFG) {

    fun parse(word: String): Boolean {
        val tokens = word.split("").filter { it.isNotEmpty() }
        val n = tokens.size

        if (n == 0) {
            return false
        }

        val P = Array(n) { Array<MutableSet<String>>(n) { mutableSetOf() } }

        for (i in 0 until n) {
            val terminal = tokens[i]
            for ((lhs, productions) in cfg.grammar) {
                for (prod in productions) {
                    if (prod.size == 1 && prod[0].type == TokenType.TERMINAL && prod[0].value == terminal) {
                        P[i][0].add(lhs)
                    }
                }
            }
        }

        for (l in 2..n) {
            for (i in 0 until n - l + 1) {
                for (k in 1 until l) {
                    val leftCell = P[i][k - 1]
                    val rightCell = P[i + k][l - k - 1]
                    for (B in leftCell) {
                        for (C in rightCell) {
                            for ((lhs, productions) in cfg.grammar) {
                                for (prod in productions) {
                                    if (prod.size == 2 && prod[0].type == TokenType.NON_TERMINAL && prod[1].type == TokenType.NON_TERMINAL) {
                                        if (prod[0].value == B && prod[1].value == C) {
                                            P[i][l - 1].add(lhs)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return P[0][n - 1].contains(cfg.startSymbol)
    }
}