package converters

import org.example.data.CFG
import org.example.data.Symbol
import org.example.data.TokenType
import java.util.LinkedList
import java.util.Queue

class CFGConverter(private val cfg: CFG) {

    private var counter = 1

    fun convertToCNF(): CFG {
        var grammar = cfg.copy()

        grammar = addNewStartSymbol(grammar)

        grammar = removeUnitProductions(grammar)

        grammar = removeUselessSymbols(grammar)

        grammar = convertToBinaryRules(grammar)

        grammar = replaceTerminalsInRules(grammar)

        return grammar
    }

    private fun addNewStartSymbol(cfg: CFG): CFG {
        val currentStartSymbol = cfg.startSymbol
        val grammar = cfg.grammar

        val newStartSymbol = generateNewNonTerminal("START_NT")

        val newRule: List<Symbol> = listOf(Symbol(TokenType.NON_TERMINAL, currentStartSymbol))
        val updatedGrammar = grammar.toMutableMap()

        updatedGrammar[newStartSymbol] = mutableListOf(newRule)

        return CFG(
            grammar = updatedGrammar,
            startSymbol = newStartSymbol,
            terminals = cfg.terminals
        )
    }


    private fun removeUnitProductions(cfg: CFG): CFG {
        val grammar = cfg.grammar.toMutableMap()

        val unitPairs = mutableMapOf<String, MutableSet<String>>()

        for (nonTerminal in grammar.keys) {
            unitPairs[nonTerminal] = mutableSetOf()
            val queue = ArrayDeque<String>()
            queue.add(nonTerminal)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                for (prod in grammar[current] ?: emptyList()) {
                    if (prod.size == 1 && prod[0].type == TokenType.NON_TERMINAL) {
                        val B = prod[0].value
                        if (B !in unitPairs[nonTerminal]!!) {
                            unitPairs[nonTerminal]!!.add(B)
                            queue.add(B)
                        }
                    }
                }
            }
        }

        val newGrammar = mutableMapOf<String, MutableList<List<Symbol>>>()

        for ((A, Bs) in unitPairs) {
            for (B in Bs) {
                for (prod in grammar[B] ?: emptyList()) {
                    if (!(prod.size == 1 && prod[0].type == TokenType.NON_TERMINAL)) {
                        newGrammar.getOrPut(A) { mutableListOf() }.add(prod)
                    }
                }
            }
        }

        for ((A, productions) in grammar) {
            for (prod in productions) {
                if (!(prod.size == 1 && prod[0].type == TokenType.NON_TERMINAL)) {
                    newGrammar.getOrPut(A) { mutableListOf() }.add(prod)
                }
            }
        }

        val finalGrammar = newGrammar.mapValues { (_, prods) ->
            prods.distinct()
        }.toMutableMap()

        return CFG(
            grammar = finalGrammar,
            startSymbol = cfg.startSymbol,
            terminals = cfg.terminals
        )
    }

    private fun removeUselessSymbols(cfg: CFG): CFG {
        val grammar = cfg.grammar.toMutableMap()
        var startSymbol = cfg.startSymbol

        val generating = findGeneratingSymbols(grammar)

        if (generating.contains(startSymbol).not()) {
            startSymbol = generating.random()
        }
        val generativeGrammar = grammar.filter { it.key in generating }
            .mapValues { (_, prods) ->
                prods.filter { prod ->
                    prod.all { symbol ->
                        symbol.type == TokenType.TERMINAL || (symbol.type == TokenType.NON_TERMINAL && symbol.value in generating)
                    }
                }
            }.toMutableMap()


        val reachable = findReachableSymbols(generativeGrammar, startSymbol)

        val useful = generating.intersect(reachable)

        val newGrammar = mutableMapOf<String, MutableList<List<Symbol>>>()
        for (nt in useful) {
            for (prod in generativeGrammar[nt] ?: emptyList()) {
                if (prod.all { symbol ->
                        symbol.type == TokenType.TERMINAL || (symbol.type == TokenType.NON_TERMINAL && symbol.value in useful)
                    }) {
                    newGrammar.getOrPut(nt) { mutableListOf() }.add(prod)
                }
            }
        }

        return CFG(
            grammar = newGrammar,
            startSymbol = startSymbol,
            terminals = cfg.terminals
        )
    }

    private fun findGeneratingSymbols(grammar: Map<String, List<List<Symbol>>>): Set<String> {
        val generating = mutableSetOf<String>()
        var changed: Boolean

        do {
            changed = false
            for ((lhs, productions) in grammar) {
                if (lhs in generating) continue
                for (prod in productions) {
                    if (prod.all { symbol ->
                            symbol.type == TokenType.TERMINAL || (symbol.type == TokenType.NON_TERMINAL && symbol.value in generating)
                        }) {
                        generating.add(lhs)
                        changed = true
                        break
                    }
                }
            }
        } while (changed)

        return generating
    }

    private fun findReachableSymbols(grammar: Map<String, List<List<Symbol>>>, startSymbol: String): Set<String> {
        val reachable = mutableSetOf<String>()
        val queue: Queue<String> = LinkedList()

        reachable.add(startSymbol)
        queue.add(startSymbol)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            for (prod in grammar[current] ?: emptyList()) {
                for (symbol in prod) {
                    if (symbol.type == TokenType.NON_TERMINAL && symbol.value !in reachable) {
                        reachable.add(symbol.value)
                        queue.add(symbol.value)
                    }
                }
            }
        }

        return reachable
    }

    private fun convertToBinaryRules(cfg: CFG): CFG {
        val grammar = cfg.grammar.toMutableMap()
        val newGrammar = mutableMapOf<String, MutableList<List<Symbol>>>()

        for ((lhs, productions) in grammar) {
            for (prod in productions) {
                if (prod.size <= 2) {
                    newGrammar.getOrPut(lhs) { mutableListOf() }.add(prod)
                } else {
                    var currentLhs = lhs
                    for (i in 0 until prod.size - 2) {
                        val first = prod[i]
                        val newNonTerminal = generateNewNonTerminal("BIN")
                        newGrammar.getOrPut(currentLhs) { mutableListOf() }.add(listOf(first, Symbol(TokenType.NON_TERMINAL, newNonTerminal)))
                        currentLhs = newNonTerminal
                    }
                    val lastTwo = prod.takeLast(2)
                    newGrammar.getOrPut(currentLhs) { mutableListOf() }.add(lastTwo)
                }
            }
        }

        return CFG(
            grammar = newGrammar,
            startSymbol = cfg.startSymbol,
            terminals = cfg.terminals
        )
    }
    private fun replaceTerminalsInRules(cfg: CFG): CFG {
        val grammar = cfg.grammar.toMutableMap()
        val newGrammar = mutableMapOf<String, MutableList<List<Symbol>>>()

        for ((lhs, productions) in grammar) {
            for (prod in productions) {
                if (prod.size >= 2) {
                    val newProd = prod.map { symbol ->
                        if (symbol.type == TokenType.TERMINAL) {
                            val existingNonTerminal = findNonTerminalForTerminal(symbol.value, newGrammar)
                            if (existingNonTerminal != null) {
                                Symbol(TokenType.NON_TERMINAL, existingNonTerminal)
                            } else {
                                val newNonTerminal = generateNewNonTerminal("TERM")
                                newGrammar[newNonTerminal] = mutableListOf(listOf(symbol))
                                Symbol(TokenType.NON_TERMINAL, newNonTerminal)
                            }
                        } else {
                            symbol
                        }
                    }
                    newGrammar.getOrPut(lhs) { mutableListOf() }.add(newProd)
                } else {
                    newGrammar.getOrPut(lhs) { mutableListOf() }.add(prod)
                }
            }
        }

        return CFG(
            grammar = newGrammar,
            startSymbol = cfg.startSymbol,
            terminals = cfg.terminals
        )
    }

    private fun findNonTerminalForTerminal(terminal: String, grammar: Map<String, MutableList<List<Symbol>>>): String? {
        for ((lhs, productions) in grammar) {
            if (productions.size == 1 && productions[0].size == 1) {
                val sym = productions[0][0]
                if (sym.type == TokenType.TERMINAL && sym.value == terminal) {
                    return lhs
                }
            }
        }
        return null
    }

    private fun generateNewNonTerminal(type: String = "NT"): String {
        while (true) {
            val newNonTerminal = "[${type}_$counter]"
            if (!cfg.grammar.containsKey(newNonTerminal)) {
                counter++
                return newNonTerminal
            }
            counter++
        }
    }
}

fun CFG.convertToCNF(): CFG {
    val converter = CFGConverter(this)
    return converter.convertToCNF()
}
