package org.example.generator

import org.example.data.CFG
import kotlin.random.Random

class BigramGenerator(
    private val cfg: CFG
) {

    fun init() : BigramGenerator {
        computeFirstSets()
        computeFollowSets()
        computeLastSets()
        computePrecedeSets()
        computeBigramMatrix()
        // bigramMatrix.forEach {
        //     println(it)
        // }
        return this
    }

    fun generateWords(wordsNumber: Int, maxLength: Int = 20) : List<String> {
        val startTerminals = firstSets[cfg.startSymbol] ?: emptySet()
        val finalTerminals = lastSets[cfg.startSymbol] ?: emptySet()

        val words = mutableSetOf<String>()

        while (words.size < wordsNumber) {
            var current = startTerminals.random()
            val wordBuilder = StringBuilder()
            wordBuilder.append(current)

            while (wordBuilder.length < maxLength) {
                val possibleNext = if (Random.nextInt(100) < 3) {
                    setOf(cfg.terminals.random().toString())
                } else {
                    bigramMatrix
                        .filter { it.first.toString() == current }
                        .map { it.second }
                        .toSet()
                }

                if (possibleNext.isEmpty()) break

                current = possibleNext.random().toString()
                wordBuilder.append(current)

                if (current in finalTerminals) {
                    if (Random.nextBoolean()) {
                        words.add(wordBuilder.toString())
                    } else {
                        break
                    }
                }
            }
            if (words.size < wordsNumber) words.add(wordBuilder.toString())
        }

        return words.toList()
    }

    private var firstSets : Map<String, Set<String>> = emptyMap()
    private var followSets : Map<String, Set<String>> = emptyMap()
    private var lastSets : Map<String, Set<String>> = emptyMap()
    private var precedeSets : Map<String, Set<String>> = emptyMap()
    private var bigramMatrix : MutableSet<Pair<Char, Char>> = mutableSetOf()

    private fun computeFirstSets() {
        val first = mutableMapOf<String, MutableSet<String>>()
        for (nonTerminal in cfg.grammar.keys) {
            first[nonTerminal] = mutableSetOf()
        }

        var changed = true
        while (changed) {
            changed = false
            for ((A, productions) in cfg.grammar) {
                for (production in productions) {
                    when (production.size) {
                        1 -> {
                            val terminal = production.first().value
                            if (first[A]?.add(terminal) == true) {
                                changed = true
                            }
                        }
                        2 -> {
                            val B = production[0].value
                            val oldSize = first[A]?.size ?: 0
                            first[A]?.addAll(first[B] ?: emptySet())
                            val newSize = first[A]?.size ?: 0
                            if (newSize > oldSize) {
                                changed = true
                            }
                        }
                    }
                }
            }
        }

        firstSets = first.mapValues { it.value.toSet() }
    }

    private fun computeFollowSets() {
        val follow = mutableMapOf<String, MutableSet<String>>()

        for (nonTerminal in cfg.grammar.keys) {
            follow[nonTerminal] = mutableSetOf()
        }

        follow[cfg.startSymbol]?.add("$")
        var changed = true
        while (changed) {
            changed = false
            for ((A, productions) in cfg.grammar) {
                for (production in productions) {
                    if (production.size == 2) {
                        val B = production[0]
                        val C = production[1]

                        run {
                            val before = follow[B.value]?.size ?: 0
                            follow[B.value]?.addAll(firstSets[C.value] ?: emptySet())
                            val after = follow[B.value]?.size ?: 0
                            if (after > before) changed = true
                        }

                        run {
                            val before = follow[C.value]?.size ?: 0
                            follow[C.value]?.addAll(follow[A] ?: emptySet())
                            val after = follow[C.value]?.size ?: 0
                            if (after > before) changed = true
                        }
                    }
                }
            }
        }
        followSets = follow
    }

    private fun computeLastSets() {
        val last = mutableMapOf<String, MutableSet<String>>()

        for (nonTerminal in cfg.grammar.keys) {
            last[nonTerminal] = mutableSetOf()
        }

        var changed = true
        while (changed) {
            changed = false
            for ((A, productions) in cfg.grammar) {
                for (production in productions) {
                    when (production.size) {
                        1 -> {
                            val terminal = production[0].value
                            if (last[A]?.add(terminal) == true) {
                                changed = true
                            }
                        }
                        2 -> {
                            val C = production[1].value
                            val lastC = last[C] ?: emptySet()
                            val beforeSize = last[A]?.size ?: 0
                            last[A]?.addAll(lastC)
                            val afterSize = last[A]?.size ?: 0
                            if (afterSize > beforeSize) {
                                changed = true
                            }
                        }
                    }
                }
            }
        }
        lastSets = last
    }

    private fun computePrecedeSets() {
        val precede = mutableMapOf<String, MutableSet<String>>()

        for (nonTerminal in cfg.grammar.keys) {
            precede[nonTerminal] = mutableSetOf()
        }

        precede[cfg.startSymbol]?.add("^")
        var changed = true
        while (changed) {
            changed = false
            for ((A, productions) in cfg.grammar) {
                for (production in productions) {
                    if (production.size == 2) {
                        val B = production[0]
                        val C = production[1]

                        run {
                            val before = precede[B.value]?.size ?: 0
                            precede[B.value]?.addAll(precede[A] ?: emptySet())
                            val after = precede[B.value]?.size ?: 0
                            if (after > before) changed = true
                        }

                        run {
                            val before = precede[C.value]?.size ?: 0
                            precede[C.value]?.addAll(lastSets[B.value] ?: emptySet())
                            val after = precede[C.value]?.size ?: 0
                            if (after > before) changed = true
                        }
                    }
                }
            }
        }
        precedeSets = precede
    }

    private fun computeBigramMatrix() {
        for (gamma1 in cfg.terminals) {
            for (gamma2 in cfg.terminals) {

                // Условие 1 нет смысла проверять, т.к. грамматика в ХНФ
                val meetsCondition2 = checkCondition2(gamma1, gamma2)
                if (meetsCondition2) {
                    bigramMatrix.add(Pair(gamma1, gamma2))
                    continue
                }
                val meetsCondition3 = checkCondition3(gamma1, gamma2)
                if (meetsCondition3) {
                    bigramMatrix.add(Pair(gamma1, gamma2))
                }
                // Условие 4 что-то абсолютно непонятное
            }
        }
    }

    private fun checkCondition2(gamma1: Char, gamma2: Char): Boolean {
        for ((A1, lastSet) in lastSets) {
            if (gamma1.toString() in lastSet && (followSets[A1]?.contains(gamma2.toString()) == true)) {
                return true
            }
        }
        return false
    }

    private fun checkCondition3(gamma1: Char, gamma2: Char): Boolean {
        for ((A2, precedeSet) in precedeSets) {
            if (gamma1.toString() in precedeSet) {
                for ((A1, firstSet) in firstSets) {
                    if (gamma2.toString() in firstSet && A1 != A2) {
                        return true
                    }
                }
            }
        }
        return false
    }

}