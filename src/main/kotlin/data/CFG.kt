package org.example.data

data class CFG(
    val grammar: Map<String, List<List<Symbol>>>,
    val startSymbol: String,
    val terminals: Set<Char>
)