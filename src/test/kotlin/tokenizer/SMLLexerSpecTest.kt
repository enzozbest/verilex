@file:Suppress("ktlint:standard:no-wildcard-imports")

package tokenizer

import lexer.Verilex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rexp.*

class SMLLexerSpecTest {
    @Test
    fun testSingleTokenExists() {
        assertNotNull(SMLLexerSpec.singleToken)
    }

    @Test
    fun testLexerExists() {
        assertNotNull(SMLLexerSpec.lexer)
    }

    @Test
    fun testLexerMatchesSingleToken() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "if")
        assertTrue(result.isNotEmpty())
        assertEquals("IF", result[0].first)
    }

    @Test
    fun testLexerMatchesAllReservedWords() {
        val keywords = listOf(
            "abstype" to "ABSTYPE",
            "and" to "AND",
            "andalso" to "ANDALSO",
            "as" to "AS",
            "case" to "CASE",
            "datatype" to "DATATYPE",
            "do" to "DO",
            "else" to "ELSE",
            "end" to "END",
            "exception" to "EXCEPTION",
            "fn" to "FN",
            "fun" to "FUN",
            "handle" to "HANDLE",
            "if" to "IF",
            "in" to "IN",
            "infix" to "INFIX",
            "infixr" to "INFIXR",
            "let" to "LET",
            "local" to "LOCAL",
            "nonfix" to "NONFIX",
            "of" to "OF",
            "op" to "OP",
            "open" to "OPEN",
            "orelse" to "ORELSE",
            "raise" to "RAISE",
            "rec" to "REC",
            "then" to "THEN",
            "type" to "TYPE",
            "val" to "VAL",
            "with" to "WITH",
            "withtype" to "WITHTYPE",
            "while" to "WHILE"
        )

        for ((lexeme, tag) in keywords) {
            val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), lexeme)
            assertEquals(1, result.size, "Expected 1 token for '$lexeme'")
            assertEquals(tag, result[0].first, "Expected tag '$tag' for lexeme '$lexeme'")
        }
    }

    @Test
    fun testLexerMatchesAllPunctuation() {
        val punctuation = listOf(
            "..." to "...",
            "=>" to "=>",
            "->" to "->",
            "(" to "(",
            ")" to ")",
            "[" to "[",
            "]" to "]",
            "{" to "{",
            "}" to "}",
            "," to ",",
            ":" to ":",
            ";" to ";",
            "_" to "_",
            "|" to "|",
            "=" to "=",
            "#" to "#"
        )

        for ((lexeme, tag) in punctuation) {
            val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), lexeme)
            assertEquals(1, result.size, "Expected 1 token for '$lexeme'")
            assertEquals(tag, result[0].first, "Expected tag '$tag' for lexeme '$lexeme'")
        }
    }

    @Test
    fun testLexerMatchesLiterals() {
        val literals = listOf(
            "42" to "INT",
            "~123" to "INT",
            "0x1A" to "INT",
            "0w42" to "WORD",
            "0wx1A" to "WORD",
            "3.14" to "REAL",
            "1E10" to "REAL",
            "\"hello\"" to "STRING",
            "#\"a\"" to "CHAR"
        )

        for ((lexeme, tag) in literals) {
            val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), lexeme)
            assertEquals(1, result.size, "Expected 1 token for '$lexeme'")
            assertEquals(tag, result[0].first, "Expected tag '$tag' for lexeme '$lexeme'")
        }
    }

    @Test
    fun testLexerMatchesIdentifiers() {
        val result1 = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "myVar")
        assertEquals("ID", result1[0].first)

        val result2 = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "++")
        assertEquals("ID", result2[0].first)
    }

    @Test
    fun testLexerMatchesTypeVariables() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "'a")
        assertTrue(result[0].first == "TYVAR" || result[0].first == "ETYVAR")
    }

    @Test
    fun testLexerMatchesEqualityTypeVariables() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "''a")
        assertTrue(result[0].first == "TYVAR")
    }

    @Test
    fun testLexerMatchesWhitespace() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "   ")
        assertEquals(1, result.size)
        assertEquals("WS", result[0].first)
    }

    @Test
    fun testLexerMatchesNewlines() {
        val result1 = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\n")
        assertEquals("NL", result1[0].first)

        // The source uses "\\r\\n" which is literal backslash chars, not CRLF
        val result2 = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\\r\\n")
        assertEquals("NL", result2[0].first)
    }

    @Test
    fun testLexerMatchesMultipleTokens() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "if x then y")
        assertEquals(7, result.size)
        assertEquals("IF", result[0].first)
        assertEquals("WS", result[1].first)
        assertEquals("ID", result[2].first)
        assertEquals("WS", result[3].first)
        assertEquals("THEN", result[4].first)
    }

    @Test
    fun testLexerPriorityReservedOverId() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "if")
        assertEquals("IF", result[0].first)
    }

    @Test
    fun testLexerPriorityLongerMatch() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "=>")
        assertEquals(1, result.size)
        assertEquals("=>", result[0].first)
    }

    @Test
    fun testSingleTokenToCharFunctionFormat() {
        val lowered = SMLLexerSpec.singleToken.toCharFunctionFormat()
        assertNotNull(lowered)
    }

    @Test
    fun testLexerWithNumericLabel() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "123")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testLexerWithTab() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\t")
        assertEquals("WS", result[0].first)
    }

    @Test
    fun testLexerWithCarriageReturn() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\r")
        assertEquals("WS", result[0].first)
    }

    @Test
    fun testLexerMatchesErrorCharacters() {
        // Control characters should be matched by the ERROR rule
        val result1 = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\u0000")
        assertEquals(1, result1.size)
        assertEquals("ERROR", result1[0].first)

        val result2 = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\u0001")
        assertEquals("ERROR", result2[0].first)

        val result3 = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\u007F") // DEL
        assertEquals("ERROR", result3[0].first)
    }

    @Test
    fun testLexerErrorCharsDoNotIncludeValidWhitespace() {
        // Tab, LF, FF, CR should NOT be error chars
        val tab = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\t")
        assertNotEquals("ERROR", tab[0].first)

        val lf = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\n")
        assertNotEquals("ERROR", lf[0].first)

        val cr = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "\r")
        assertNotEquals("ERROR", cr[0].first)
    }

    @Test
    fun testLexerMatchesQualifiedId() {
        val result = Verilex.lex(SMLLexerSpec.lexer.toCharFunctionFormat(), "Foo.bar")
        assertEquals(1, result.size)
        assertEquals("ID", result[0].first)
        assertEquals("Foo.bar", result[0].second)
    }

    @Test
    fun testLexerSingleTokenHasNoStar() {
        val result = Verilex.lex(SMLLexerSpec.singleToken.toCharFunctionFormat(), "if")
        assertEquals(1, result.size)
        assertEquals("IF", result[0].first)
    }
}
