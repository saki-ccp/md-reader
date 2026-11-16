package com.aryan.reader.epub

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Test

class DelimitedTextParserTest {
    private fun parse(text: String, delimiter: Char = ','): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        readDelimitedRows(StringReader(text), delimiter) { rows.add(it) }
        return rows
    }

    @Test
    fun `quoted commas and escaped quotes preserve cell content`() {
        assertEquals(listOf(listOf("note, one", "He said \"hello\"")), parse("\"note, one\",\"He said \"\"hello\"\"\""))
    }

    @Test
    fun `newlines inside quoted cells stay in one row`() {
        assertEquals(listOf(listOf("line one\r\nline two", "value"), listOf("next", "row")),
            parse("\"line one\r\nline two\",value\r\nnext,row\r\n"))
    }

    @Test
    fun `empty cells bom and trailing delimiter are preserved`() {
        assertEquals(listOf(listOf("", "a", "")), parse("\uFEFF,a,"))
        assertEquals(emptyList<List<String>>(), parse(""))
        assertEquals(listOf(listOf("")), parse("\"\""))
    }

    @Test
    fun `tsv uses tabs as delimiters`() {
        assertEquals(listOf(listOf("a,b", "c\td")), parse("a,b\t\"c\td\"", '\t'))
    }
}
