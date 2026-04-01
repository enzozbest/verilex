@file:Suppress("ktlint:standard:no-wildcard-imports")

package tokenizer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Concrete subclass that exposes [Tokeniser]'s protected helper methods for unit testing.
 */
private class TestableTokeniser : Tokeniser<List<Token>>() {
    override fun tokenise(source: String): List<Token> = buildTokenList(emptyList())

    fun testBuildTokenList(rawTokens: List<Pair<String, String>>): List<Token> = buildTokenList(rawTokens)

    fun testCreateLexicalError(source: String, cause: IllegalStateException): TokenisationError =
        createLexicalError(source, cause)

    fun testFindFailurePosition(source: String): Int = findFailurePosition(source)

    fun testCanTokenise(source: String): Boolean = canTokenise(source)

    fun testExtractContext(source: String, position: Int, windowSize: Int = 20): String =
        extractContext(source, position, windowSize)
}

class TokeniserTest {
    private val tokeniser = TestableTokeniser()

    // ---------------------------------------------------------------
    // buildTokenList
    // ---------------------------------------------------------------
    @Nested
    inner class BuildTokenListTests {
        @Test
        fun `empty list returns empty token list`() {
            val result = tokeniser.testBuildTokenList(emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `single token has position zero`() {
            val result = tokeniser.testBuildTokenList(listOf("IF" to "if"))
            assertEquals(1, result.size)
            assertEquals(SMLTokenType.ReservedWord.IF, result[0].type)
            assertEquals("if", result[0].lexeme)
            assertEquals(0, result[0].position)
        }

        @Test
        fun `multiple tokens track positions correctly`() {
            val rawTokens = listOf(
                "VAL" to "val",
                "WS" to " ",
                "ID" to "x",
                "WS" to " ",
                "=" to "=",
                "WS" to " ",
                "INT" to "42",
            )
            val result = tokeniser.testBuildTokenList(rawTokens)

            assertEquals(7, result.size)

            // "val" starts at 0, length 3
            assertEquals(0, result[0].position)
            assertEquals("val", result[0].lexeme)

            // " " starts at 3, length 1
            assertEquals(3, result[1].position)

            // "x" starts at 4, length 1
            assertEquals(4, result[2].position)
            assertEquals("x", result[2].lexeme)

            // " " starts at 5
            assertEquals(5, result[3].position)

            // "=" starts at 6
            assertEquals(6, result[4].position)

            // " " starts at 7
            assertEquals(7, result[5].position)

            // "42" starts at 8, length 2
            assertEquals(8, result[6].position)
            assertEquals("42", result[6].lexeme)
        }

        @Test
        fun `tokens have correct types`() {
            val rawTokens = listOf(
                "IF" to "if",
                "WS" to " ",
                "ID" to "x",
            )
            val result = tokeniser.testBuildTokenList(rawTokens)

            assertEquals(SMLTokenType.ReservedWord.IF, result[0].type)
            assertEquals(SMLTokenType.Trivia.WHITESPACE, result[1].type)
            assertEquals(SMLTokenType.Identifier.IDENTIFIER, result[2].type)
        }
    }

    // ---------------------------------------------------------------
    // canTokenise
    // ---------------------------------------------------------------
    @Nested
    inner class CanTokeniseTests {
        @Test
        fun `empty string is always tokenisable`() {
            assertTrue(tokeniser.testCanTokenise(""))
        }

        @Test
        fun `valid SML expression is tokenisable`() {
            assertTrue(tokeniser.testCanTokenise("val x = 42"))
        }

        @Test
        fun `valid SML reserved word is tokenisable`() {
            assertTrue(tokeniser.testCanTokenise("if"))
        }

        @Test
        fun `lone dot is not tokenisable`() {
            assertFalse(tokeniser.testCanTokenise("."))
        }

        @Test
        fun `lone double quote is not tokenisable`() {
            assertFalse(tokeniser.testCanTokenise("\""))
        }
    }

    // ---------------------------------------------------------------
    // findFailurePosition
    // ---------------------------------------------------------------
    @Nested
    inner class FindFailurePositionTests {
        @Test
        fun `finds position of invalid character in middle of valid source`() {
            // "val " is valid (4 chars), then "." fails
            val source = "val ."
            val pos = tokeniser.testFindFailurePosition(source)
            // The binary search should converge near the "." at index 4
            assertTrue(pos >= 4, "Expected failure position >= 4 but got $pos")
        }

        @Test
        fun `invalid character at start returns low position`() {
            val source = "."
            val pos = tokeniser.testFindFailurePosition(source)
            // Single-character invalid string: position should be 0 or 1
            assertTrue(pos <= 1, "Expected failure position <= 1 but got $pos")
        }

        @Test
        fun `position is clamped to valid range`() {
            val source = "abc"
            val pos = tokeniser.testFindFailurePosition(source)
            assertTrue(pos in 0 until source.length, "Position $pos out of range for source of length ${source.length}")
        }
    }

    // ---------------------------------------------------------------
    // createLexicalError
    // ---------------------------------------------------------------
    @Nested
    inner class CreateLexicalErrorTests {
        @Test
        fun `creates error with correct message format`() {
            val cause = IllegalStateException("test cause")
            val error = tokeniser.testCreateLexicalError("val . x", cause)

            assertTrue(error.message!!.startsWith("Unexpected character at position"))
            assertSame(cause, error.cause)
        }

        @Test
        fun `error contains position and context`() {
            val cause = IllegalStateException("test cause")
            val source = "val . x"
            val error = tokeniser.testCreateLexicalError(source, cause)

            assertTrue(error.position >= 0)
            assertTrue(error.context.isNotEmpty())
        }

        @Test
        fun `error wraps the original cause`() {
            val cause = IllegalStateException("original error")
            val error = tokeniser.testCreateLexicalError(".", cause)

            assertNotNull(error.cause)
            assertEquals("original error", error.cause!!.message)
        }
    }

    // ---------------------------------------------------------------
    // extractContext
    // ---------------------------------------------------------------
    @Nested
    inner class ExtractContextTests {
        @Test
        fun `returns full string when shorter than window`() {
            val result = tokeniser.testExtractContext("hello", 2, 20)
            assertEquals("hello", result)
        }

        @Test
        fun `adds prefix ellipsis when start is trimmed`() {
            val source = "abcdefghijklmnopqrstuvwxyz0123456789"
            val result = tokeniser.testExtractContext(source, 20, 10)
            assertTrue(result.startsWith("..."), "Expected prefix ellipsis, got: $result")
        }

        @Test
        fun `adds suffix ellipsis when end is trimmed`() {
            val source = "abcdefghijklmnopqrstuvwxyz0123456789"
            val result = tokeniser.testExtractContext(source, 5, 10)
            assertTrue(result.endsWith("..."), "Expected suffix ellipsis, got: $result")
        }

        @Test
        fun `adds both ellipses for middle of long string`() {
            val source = "a".repeat(100)
            val result = tokeniser.testExtractContext(source, 50, 10)
            assertTrue(result.startsWith("..."), "Expected prefix ellipsis")
            assertTrue(result.endsWith("..."), "Expected suffix ellipsis")
        }

        @Test
        fun `no ellipsis when entire string fits in window`() {
            val result = tokeniser.testExtractContext("hi", 1, 20)
            assertFalse(result.startsWith("..."))
            assertFalse(result.endsWith("..."))
            assertEquals("hi", result)
        }

        @Test
        fun `replaces newlines with escaped representation`() {
            val result = tokeniser.testExtractContext("line1\nline2", 5, 40)
            assertTrue(result.contains("\\n"), "Expected escaped newline, got: $result")
            assertFalse(result.contains("\n"), "Should not contain literal newline")
        }

        @Test
        fun `handles position at start of string`() {
            val source = "abcdefghijklmnopqrstuvwxyz"
            val result = tokeniser.testExtractContext(source, 0, 10)
            // start coerced to 0, no prefix ellipsis
            assertFalse(result.startsWith("..."))
        }

        @Test
        fun `handles position at end of string`() {
            val source = "abcdefghijklmnopqrstuvwxyz"
            val result = tokeniser.testExtractContext(source, source.length - 1, 10)
            // end coerced to source.length, no suffix ellipsis
            assertFalse(result.endsWith("..."))
        }

        @Test
        fun `custom window size is respected`() {
            val source = "a".repeat(100)
            val smallWindow = tokeniser.testExtractContext(source, 50, 6)
            val largeWindow = tokeniser.testExtractContext(source, 50, 40)
            // Strip ellipses to compare content lengths
            val smallContent = smallWindow.removePrefix("...").removeSuffix("...")
            val largeContent = largeWindow.removePrefix("...").removeSuffix("...")
            assertTrue(
                smallContent.length <= largeContent.length,
                "Small window ($smallContent) should produce less content than large window ($largeContent)",
            )
        }

        @Test
        fun `handles empty string`() {
            val result = tokeniser.testExtractContext("", 0, 20)
            assertEquals("", result)
        }
    }
}
