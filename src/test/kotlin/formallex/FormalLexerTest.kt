package formallex

import formallex.KotlinScalaConverter.toIsabelleChar
import formallex.KotlinScalaConverter.toIsabelleList
import formallex.KotlinScalaConverter.toIsabelleNat
import formallex.KotlinScalaConverter.toKotlinChar
import formallex.KotlinScalaConverter.toKotlinValue
import formallex.KotlinScalaConverter.toRexp
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import rexp.*
import scala.jdk.javaapi.CollectionConverters
import value.*
import verified.SLULexer

class FormalLexerTest {

    // ---------------------------------------------------------------
    // 1. Character conversion round-trip
    // ---------------------------------------------------------------

    @Nested
    inner class CharConversionTests {

        @Test
        fun `printable ASCII characters survive round-trip`() {
            for (code in 32..126) {
                val c = code.toChar()
                val roundTripped = c.toIsabelleChar().toKotlinChar()
                assertEquals(c, roundTripped, "Round-trip failed for char '$c' (code $code)")
            }
        }

        @Test
        fun `boundary character 0 survives round-trip`() {
            val c = 0.toChar()
            assertEquals(c, c.toIsabelleChar().toKotlinChar())
        }

        @Test
        fun `boundary character 127 survives round-trip`() {
            val c = 127.toChar()
            assertEquals(c, c.toIsabelleChar().toKotlinChar())
        }

        @Test
        fun `boundary character 255 survives round-trip`() {
            val c = 255.toChar()
            assertEquals(c, c.toIsabelleChar().toKotlinChar())
        }

        @Test
        fun `specific characters encode correctly`() {
            // 'A' = 65 = 0b01000001
            val isabelleA = 'A'.toIsabelleChar() as SLULexer.Char
            assertTrue(isabelleA.a())   // bit 0 = 1
            assertFalse(isabelleA.b())  // bit 1 = 0
            assertFalse(isabelleA.c())  // bit 2 = 0
            assertFalse(isabelleA.d())  // bit 3 = 0
            assertFalse(isabelleA.e())  // bit 4 = 0
            assertFalse(isabelleA.f())  // bit 5 = 0
            assertTrue(isabelleA.g())   // bit 6 = 1
            assertFalse(isabelleA.h())  // bit 7 = 0
        }
    }

    // ---------------------------------------------------------------
    // 2. String to Isabelle list
    // ---------------------------------------------------------------

    @Nested
    inner class StringToIsabelleListTests {

        @Test
        fun `empty string produces empty list`() {
            val list = "".toIsabelleList()
            assertTrue(list.isEmpty)
        }

        @Test
        fun `single character string produces singleton list`() {
            val list = "a".toIsabelleList()
            assertEquals(1, list.size())
            assertEquals('a', (list.head() as SLULexer.char).toKotlinChar())
        }

        @Test
        fun `multi-character string has correct length`() {
            val input = "hello"
            val list = input.toIsabelleList()
            assertEquals(5, list.size())
        }

        @Test
        fun `string round-trips through Isabelle list`() {
            val input = "hello world"
            val list = input.toIsabelleList()
            val recovered = CollectionConverters.asJava(list).map { it.toKotlinChar() }.joinToString("")
            assertEquals(input, recovered)
        }
    }

    // ---------------------------------------------------------------
    // 3. Int to Isabelle nat
    // ---------------------------------------------------------------

    @Nested
    inner class IntToIsabelleNatTests {

        @Test
        fun `zero converts to zero_nat`() {
            val nat = 0.toIsabelleNat()
            // zero_nat is not a Suc
            assertFalse(nat is SLULexer.Suc)
        }

        @Test
        fun `one converts to Suc of zero`() {
            val nat = 1.toIsabelleNat()
            assertTrue(nat is SLULexer.Suc)
            val inner = (nat as SLULexer.Suc).a()
            assertFalse(inner is SLULexer.Suc)
        }

        @Test
        fun `five converts to five nested Sucs`() {
            var nat = 5.toIsabelleNat()
            var count = 0
            while (nat is SLULexer.Suc) {
                count++
                nat = nat.a()
            }
            assertEquals(5, count)
        }
    }

    // ---------------------------------------------------------------
    // 4. Regex conversion (toRexp)
    // ---------------------------------------------------------------

    @Nested
    inner class RegexConversionTests {

        @Test
        fun `ZERO converts to SLULexer Zero`() {
            val result = ZERO.toRexp()
            assertTrue(result is SLULexer.Zero<*>)
        }

        @Test
        fun `ONE converts to SLULexer One`() {
            val result = ONE.toRexp()
            assertTrue(result is SLULexer.One<*>)
        }

        @Test
        fun `CHAR converts to SLULexer Atom`() {
            val result = CHAR('a').toRexp()
            assertTrue(result is SLULexer.Atom<*>)
            val atom = result as SLULexer.Atom<SLULexer.char>
            assertEquals('a', atom.a().toKotlinChar())
        }

        @Test
        fun `RANGE converts to SLULexer Charset`() {
            val result = RANGE(setOf('a', 'b')).toRexp()
            assertTrue(result is SLULexer.Charset<*>)
        }

        @Test
        fun `ALT converts to SLULexer Plus`() {
            val result = ALT(CHAR('a'), CHAR('b')).toRexp()
            assertTrue(result is SLULexer.Plus<*>)
        }

        @Test
        fun `SEQ converts to SLULexer Times`() {
            val result = SEQ(CHAR('a'), CHAR('b')).toRexp()
            assertTrue(result is SLULexer.Times<*>)
        }

        @Test
        fun `STAR converts to SLULexer Star`() {
            val result = STAR(CHAR('a')).toRexp()
            assertTrue(result is SLULexer.Star<*>)
        }

        @Test
        fun `PLUS desugars to Times of r and Star of r`() {
            val result = PLUS(CHAR('a')).toRexp()
            assertTrue(result is SLULexer.Times<*>, "PLUS should desugar to Times")
            val times = result as SLULexer.Times<SLULexer.char>
            assertTrue(times.a() is SLULexer.Atom<*>, "First part should be Atom")
            assertTrue(times.b() is SLULexer.Star<*>, "Second part should be Star")
        }

        @Test
        fun `RECD converts to SLULexer Rec`() {
            val result = RECD("tag", CHAR('a')).toRexp()
            assertTrue(result is SLULexer.Rec<*>)
        }

        @Test
        fun `CFUN throws IllegalStateException`() {
            val cfun = CFUN("test") { it == 'a' }
            assertThrows<IllegalStateException> {
                cfun.toRexp()
            }
        }
    }

    // ---------------------------------------------------------------
    // 5. Value conversion (toKotlinValue)
    // ---------------------------------------------------------------

    @Nested
    inner class ValueConversionTests {

        @Test
        fun `Void converts to Empty`() {
            val void = SLULexer.Void<SLULexer.char>()
            val result = void.toKotlinValue()
            assertEquals(Empty, result)
        }

        @Test
        fun `Atm converts to Chr`() {
            val atm = SLULexer.Atm<SLULexer.char>('x'.toIsabelleChar())
            val result = atm.toKotlinValue()
            assertTrue(result is Chr)
            assertEquals('x', (result as Chr).c)
        }

        @Test
        fun `Seq converts to Seq`() {
            val seq = SLULexer.Seq<SLULexer.char>(
                SLULexer.Atm<SLULexer.char>('a'.toIsabelleChar()),
                SLULexer.Atm<SLULexer.char>('b'.toIsabelleChar()),
            )
            val result = seq.toKotlinValue()
            assertTrue(result is value.Seq)
            val seqVal = result as value.Seq
            assertEquals("a", seqVal.v1.flatten())
            assertEquals("b", seqVal.v2.flatten())
        }

        @Test
        fun `Left converts to Left`() {
            val left = SLULexer.Left<SLULexer.char>(
                SLULexer.Atm<SLULexer.char>('a'.toIsabelleChar()),
            )
            val result = left.toKotlinValue()
            assertTrue(result is value.Left)
            assertEquals("a", result.flatten())
        }

        @Test
        fun `Right converts to Right`() {
            val right = SLULexer.Right<SLULexer.char>(
                SLULexer.Atm<SLULexer.char>('b'.toIsabelleChar()),
            )
            val result = right.toKotlinValue()
            assertTrue(result is value.Right)
            assertEquals("b", result.flatten())
        }

        @Test
        fun `Stars converts to Stars`() {
            val items: List<SLULexer.vala<SLULexer.char>> = listOf(
                SLULexer.Atm<SLULexer.char>('a'.toIsabelleChar()),
                SLULexer.Atm<SLULexer.char>('b'.toIsabelleChar()),
            )
            val scalaList = scala.jdk.javaapi.CollectionConverters.asScala(items).toList()
            val stars = SLULexer.Stars<SLULexer.char>(scalaList)
            val result = stars.toKotlinValue()
            assertTrue(result is value.Stars)
            assertEquals("ab", result.flatten())
        }

        @Test
        fun `Recv converts to Rec`() {
            val tagChars = "tag".map { it.toIsabelleChar() }
            val scalaTag = scala.jdk.javaapi.CollectionConverters.asScala(tagChars).toList()
            val recv = SLULexer.Recv<SLULexer.char>(
                scalaTag,
                SLULexer.Atm<SLULexer.char>('x'.toIsabelleChar()),
            )
            val result = recv.toKotlinValue()
            assertTrue(result is value.Rec)
            val rec = result as value.Rec
            assertEquals("tag", rec.x)
            assertEquals("x", rec.v.flatten())
        }
    }

    // ---------------------------------------------------------------
    // 6. FormalLexer.verifiedLex integration tests
    // ---------------------------------------------------------------

    @Nested
    inner class VerifiedLexTests {

        @Test
        fun `CHAR matches single character`() {
            val result = FormalLexer.verifiedLex(CHAR('a'), "a")
            assertNotNull(result)
            assertEquals("a", result!!.flatten())
        }

        @Test
        fun `CHAR does not match wrong character`() {
            val result = FormalLexer.verifiedLex(CHAR('a'), "b")
            assertNull(result)
        }

        @Test
        fun `CHAR does not match empty string`() {
            val result = FormalLexer.verifiedLex(CHAR('a'), "")
            assertNull(result)
        }

        @Test
        fun `RECD produces correct env`() {
            val result = FormalLexer.verifiedLex(RECD("tag", CHAR('a')), "a")
            assertNotNull(result)
            val env = result!!.env()
            assertEquals(1, env.size)
            assertEquals("tag", env[0].first)
            assertEquals("a", env[0].second)
        }

        @Test
        fun `ALT matches first alternative`() {
            val regex = ALT(CHAR('a'), CHAR('b'))
            val result = FormalLexer.verifiedLex(regex, "a")
            assertNotNull(result)
            assertEquals("a", result!!.flatten())
        }

        @Test
        fun `ALT matches second alternative`() {
            val regex = ALT(CHAR('a'), CHAR('b'))
            val result = FormalLexer.verifiedLex(regex, "b")
            assertNotNull(result)
            assertEquals("b", result!!.flatten())
        }

        @Test
        fun `ALT does not match unrelated character`() {
            val regex = ALT(CHAR('a'), CHAR('b'))
            val result = FormalLexer.verifiedLex(regex, "c")
            assertNull(result)
        }

        @Test
        fun `SEQ matches concatenation`() {
            val regex = SEQ(CHAR('a'), CHAR('b'))
            val result = FormalLexer.verifiedLex(regex, "ab")
            assertNotNull(result)
            assertEquals("ab", result!!.flatten())
        }

        @Test
        fun `SEQ does not match partial input`() {
            val regex = SEQ(CHAR('a'), CHAR('b'))
            val result = FormalLexer.verifiedLex(regex, "a")
            assertNull(result)
        }

        @Test
        fun `STAR matches empty string`() {
            val regex = STAR(CHAR('a'))
            val result = FormalLexer.verifiedLex(regex, "")
            assertNotNull(result)
            assertEquals("", result!!.flatten())
        }

        @Test
        fun `STAR matches one repetition`() {
            val regex = STAR(CHAR('a'))
            val result = FormalLexer.verifiedLex(regex, "a")
            assertNotNull(result)
            assertEquals("a", result!!.flatten())
        }

        @Test
        fun `STAR matches multiple repetitions`() {
            val regex = STAR(CHAR('a'))
            val result = FormalLexer.verifiedLex(regex, "aaa")
            assertNotNull(result)
            assertEquals("aaa", result!!.flatten())
        }

        @Test
        fun `RANGE matches character in range`() {
            val regex = RANGE(setOf('a', 'b', 'c'))
            val result = FormalLexer.verifiedLex(regex, "b")
            assertNotNull(result)
            assertEquals("b", result!!.flatten())
        }

        @Test
        fun `RANGE does not match character outside range`() {
            val regex = RANGE(setOf('a', 'b', 'c'))
            val result = FormalLexer.verifiedLex(regex, "z")
            assertNull(result)
        }

        @Test
        fun `PLUS matches one character`() {
            val regex = PLUS(CHAR('a'))
            val result = FormalLexer.verifiedLex(regex, "a")
            assertNotNull(result)
            assertEquals("a", result!!.flatten())
        }

        @Test
        fun `PLUS matches multiple characters`() {
            val regex = PLUS(CHAR('a'))
            val result = FormalLexer.verifiedLex(regex, "aaa")
            assertNotNull(result)
            assertEquals("aaa", result!!.flatten())
        }

        @Test
        fun `PLUS does not match empty string`() {
            val regex = PLUS(CHAR('a'))
            val result = FormalLexer.verifiedLex(regex, "")
            assertNull(result)
        }

        @Test
        fun `complex regex with tagged sequence`() {
            val regex = SEQ(
                RECD("first", CHAR('a')),
                RECD("second", CHAR('b')),
            )
            val result = FormalLexer.verifiedLex(regex, "ab")
            assertNotNull(result)
            val env = result!!.env()
            assertEquals(2, env.size)
            assertEquals("first" to "a", env[0])
            assertEquals("second" to "b", env[1])
        }

        @Test
        fun `ONE matches empty string`() {
            val result = FormalLexer.verifiedLex(ONE, "")
            assertNotNull(result)
            assertEquals("", result!!.flatten())
        }

        @Test
        fun `ZERO matches nothing`() {
            val result = FormalLexer.verifiedLex(ZERO, "")
            assertNull(result)
        }
    }
}
