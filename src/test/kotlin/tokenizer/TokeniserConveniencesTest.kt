@file:Suppress("ktlint:standard:no-wildcard-imports")

package tokenizer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TokeniserConveniencesTest {
    @Test
    fun testTokenizeSMLSuccess() {
        val result = "val x = 1".tokenizeSML()
        assertTrue(result.isNotEmpty())
        assertEquals(SMLTokenType.ReservedWord.VAL, result.withoutTrivia()[0].type)
    }

    @Test
    fun testTokenizeSMLEmpty() {
        val result = "".tokenizeSML()
        assertTrue(result.isEmpty())
    }

    @Test
    fun testTokenizeSMLWithUnexpectedChar() {
        val result = "\u0000invalid".tokenizeSML()
        assertTrue(result.isNotEmpty())
        assertEquals(SMLTokenType.Error.ERROR, result[0].type)
    }

    @Test
    fun testTokenizeSMLOrNullSuccess() {
        val result = "val x = 1".tokenizeSMLOrNull()
        assertNotNull(result)
        assertTrue(result!!.isNotEmpty())
    }

    @Test
    fun testTokenizeSMLOrNullEmpty() {
        val result = "".tokenizeSMLOrNull()
        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun testTokenizeSMLOrNullWithUnexpectedChar() {
        val result = "\u0000invalid".tokenizeSMLOrNull()
        assertNotNull(result)
        assertEquals(SMLTokenType.Error.ERROR, result!![0].type)
    }

    @Test
    fun testTokenizeSMLChaining() {
        val compact = "if x then y".tokenizeSML().withoutTrivia().toCompactString()
        assertTrue(compact.contains("IF"))
    }
}
