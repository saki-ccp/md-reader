package com.aryan.reader.epub

import java.io.PushbackReader
import java.io.Reader

internal fun readDelimitedRows(reader: Reader, delimiter: Char, onRow: (List<String>) -> Unit) {
    val input = PushbackReader(reader, 1)
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var rowStarted = false
    var firstCharacter = true

    while (true) {
        val value = input.read()
        if (value == -1) break
        val char = value.toChar()
        if (firstCharacter && char == '\uFEFF') {
            firstCharacter = false
            continue
        }
        firstCharacter = false
        rowStarted = true
        if (inQuotes) {
            if (char == '"') {
                val next = input.read()
                if (next == '"'.code) field.append('"') else {
                    inQuotes = false
                    if (next != -1) input.unread(next)
                }
            } else {
                field.append(char)
            }
        } else {
            when (char) {
                '"' -> if (field.isEmpty()) inQuotes = true else field.append(char)
                delimiter -> {
                    fields.add(field.toString())
                    field.clear()
                }
                '\r', '\n' -> {
                    if (char == '\r') {
                        val next = input.read()
                        if (next != -1 && next != '\n'.code) input.unread(next)
                    }
                    fields.add(field.toString())
                    onRow(fields.toList())
                    fields.clear()
                    field.clear()
                    rowStarted = false
                }
                else -> field.append(char)
            }
        }
    }
    if (rowStarted) {
        fields.add(field.toString())
        onRow(fields.toList())
    }
}
