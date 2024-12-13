package org.example.generator

import org.example.parser.CYKParser

fun generateTests(words: List<String>, parser: CYKParser) : String {
    val s = StringBuilder()
    words.forEach {
        s.append("$it ")
        if (parser.parse(it)) {
            s.append("1\n")
        } else {
            s.append("0\n")
        }
    }
    return s.toString()
}