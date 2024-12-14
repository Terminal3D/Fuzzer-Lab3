package org.example

import converters.convertToCNF
import org.example.generator.BFSGenerator
import org.example.generator.BigramGenerator
import org.example.generator.generateTests
import org.example.parser.CYKParser
import org.example.parser.GrammarParser

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val testCfg = """
        E -> T E1
        E1 -> + T E1
        E1 -> - T E1
        E1 -> e
        T  -> F T1
        T1 -> * F T1
        T1 -> / F T1
        T1 -> e
        F -> - F1
        F -> F1
        F1  -> [number]
        F1 -> [var]
        F1 -> ( E )
        [var] -> x
        [var] -> y
        [var] -> z
        [number] -> 0 [number1]
        [number] -> 1 [number1]
        [number] -> 2 [number1]
        [number] -> 3 [number1]
        [number] -> 4 [number1]
        [number] -> 5 [number1]
        [number] -> 6 [number1]
        [number] -> 7 [number1]
        [number] -> 8 [number1]
        [number] -> 9 [number1]
        [number1] -> 0 [number1]
        [number1] -> 1 [number1]
        [number1] -> 2 [number1]
        [number1] -> 3 [number1]
        [number1] -> 4 [number1]
        [number1] -> 5 [number1]
        [number1] -> 6 [number1]
        [number1] -> 7 [number1]
        [number1] -> 8 [number1]
        [number1] -> 9 [number1]
        [number1] -> e
    """.trimIndent()

    val testCfg2 = """
        S -> a X b X
        S -> a Z
        X -> a Y 
        X -> b Y
        X -> t
        Y -> X
        Y -> c c
        Z -> Z X
    """.trimIndent()

    val testCfg3 = """
        S -> S a [S]
        S -> C C
        C -> a C
        S -> a
        [S] -> c c c S
    """.trimIndent()

    val parser = GrammarParser()
    val parsedGrammar = parser.parse(testCfg3)

    val cnfGrammar = parsedGrammar.convertToCNF()

    println("\nГрамматика после конвертации в ХНФ :")
    println("Начальный символ: ${cnfGrammar.startSymbol}")
    for ((lhs, productions) in cnfGrammar.grammar) {
        println("$lhs ->")
        for (prod in productions) {
            println("   ${prod.joinToString(", ")}")
        }
    }



    val bigramGenerator = BigramGenerator(cnfGrammar).init()
    val bfsGenerator = BFSGenerator(cnfGrammar)

    // val words = bigramGenerator.generateWords(wordsNumber = 2000)

    val words = bfsGenerator.generateWords(2, alwaysPositive = true)

    val cykParser = CYKParser(cnfGrammar)

    val tests = generateTests(words, cykParser)
    println(tests)
}