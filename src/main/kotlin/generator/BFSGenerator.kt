package org.example.generator

import org.example.data.CFG
import org.example.data.Symbol
import org.example.data.TokenType
import java.util.LinkedList

class BFSGenerator(
    private val cfg: CFG,
) {

    data class QueueItem(
        val word: LinkedList<Symbol>,
        val firstNtPos: Int = 0
    )

    private val generatedWords = mutableSetOf<String>()

    fun generateWords(wordsNumber: Int, wordLength: Int? = null, alwaysPositive: Boolean = false): List<String> {
        val queue = ArrayDeque<QueueItem>()
        queue.add(
            QueueItem(
                word = LinkedList<Symbol>().apply {
                    this.add(
                        Symbol(
                            type = TokenType.NON_TERMINAL,
                            value = cfg.startSymbol
                        )
                    )
                },
            )
        )
        while (generatedWords.size < wordsNumber && queue.isNotEmpty()) {
            val item = queue.removeFirst()
            if (item.firstNtPos == item.word.size) {
                if (wordLength == null || item.word.size == wordLength) {
                    generatedWords.add(item.word.joinToString(separator = "") { it.value })
                }
                continue
            }
            val nt = item.word[item.firstNtPos]
            val prods = cfg.grammar[nt.value] ?: continue
            for (prod in prods) {
                val newList = item.word.clone() as LinkedList<Symbol>
                if (prod.size == 1) {
                    newList[item.firstNtPos] = prod[0]
                    queue.add(
                        QueueItem(
                            word = newList,
                            firstNtPos = item.firstNtPos + 1
                        )
                    )
                } else {
                    newList[item.firstNtPos] = prod[0]
                    newList.add(item.firstNtPos + 1, prod[1])
                    if (wordLength != null && newList.size > wordLength) {
                        if (!alwaysPositive) {
                            generatedWords.add(
                                newList.map {
                                    if (it.type == TokenType.NON_TERMINAL) {
                                        Symbol(
                                            type = TokenType.TERMINAL,
                                            value = cfg.terminals.random().toString()
                                        )
                                    } else it
                                }.joinToString(separator = "") { it.value }
                            )
                        }
                        continue
                    }
                    queue.add(
                        QueueItem(
                            word = newList,
                            firstNtPos = item.firstNtPos
                        )
                    )
                }
            }
        }
        return generatedWords.toList()
    }
}