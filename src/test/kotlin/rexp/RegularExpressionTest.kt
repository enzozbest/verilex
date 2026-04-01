@file:Suppress("ktlint:standard:no-wildcard-imports")

package rexp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import value.*

class RegularExpressionTest {
    @Test
    fun testZERO() {
        val r = ZERO
        assertEquals("ZERO", r.toString())
        assertFalse(r.nullable())
        assertEquals(ZERO, r.der('a'))

        val (rs, rect) = r.simp()
        assertEquals(ZERO, rs)
        assertEquals(Empty, rect(Empty)) // id
        assertEquals(ZERO, r.toCharFunctionFormat())
        assertThrows(IllegalStateException::class.java) { r.mkeps() }
    }

    @Test
    fun testONE() {
        val r = ONE
        assertEquals("ONE", r.toString())
        assertTrue(r.nullable())
        assertEquals(ZERO, r.der('a'))

        val (rs, rect) = r.simp()
        assertEquals(ONE, rs)
        assertEquals(Empty, rect(Empty)) // id
        assertEquals(ONE, r.toCharFunctionFormat())
        assertEquals(Empty, r.mkeps())
    }

    @Test
    fun testCHAR() {
        val r = CHAR('a')
        assertEquals("[a]", r.toString())
        val lowered = r.toCharFunctionFormat()
        assertTrue(lowered is CFUN)
        assertEquals("[a]", lowered.toString())

        // Before lowering, these should throw
        assertThrows(IllegalStateException::class.java) { r.nullable() }
        assertThrows(IllegalStateException::class.java) { r.der('a') }
        assertThrows(IllegalStateException::class.java) { r.simp() }
        assertThrows(IllegalStateException::class.java) { r.mkeps() }
    }

    @Test
    fun testRANGE() {
        val r = RANGE(setOf('a', 'b'))
        assertEquals("[a,b]", r.toString())

        val lowered = r.toCharFunctionFormat()
        assertTrue(lowered is CFUN)
        assertEquals("[a,b]", lowered.toString())
        assertEquals(ONE, lowered.der('a'))
        assertEquals(ONE, lowered.der('b'))
        assertEquals(ZERO, lowered.der('c'))

        // Before lowering, these should throw
        assertThrows(IllegalStateException::class.java) { r.nullable() }
        assertThrows(IllegalStateException::class.java) { r.der('a') }
        assertThrows(IllegalStateException::class.java) { r.simp() }
        assertThrows(IllegalStateException::class.java) { r.mkeps() }
    }

    @Test
    fun testCFUN() {
        val r = CFUN("test") { it == 'a' }
        assertEquals("test", r.toString())
        assertFalse(r.nullable())
        assertEquals(ONE, r.der('a'))
        assertEquals(ZERO, r.der('b'))

        val (rs, rect) = r.simp()
        assertEquals(r, rs)
        assertEquals(Empty, rect(Empty))
        assertEquals(r, r.toCharFunctionFormat())
        assertThrows(IllegalStateException::class.java) { r.mkeps() }
    }

    @Test
    fun testCFUNEquality() {
        val f: (Char) -> Boolean = { it == 'a' }
        val r1 = CFUN("test", f)
        val r2 = CFUN("test", f)
        // Same lambda instance → equal
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())

        // Different lambda instances with same name → not equal (lambdas compare by identity)
        val r3 = CFUN("test") { it == 'a' }
        val r4 = CFUN("test") { it == 'a' }
        assertNotEquals(r3, r4)

        // Different name → not equal
        val r5 = CFUN("other", f)
        assertNotEquals(r1, r5)

        // Test against null and different types for full equals branch coverage
        assertFalse(r1.equals(null))
        assertFalse(r1.equals("not a CFUN"))
        assertTrue(r1.equals(r1)) // identity

        // Same name, different lambda → not equal (exercises asString-equal, f-not-equal branch)
        val g: (Char) -> Boolean = { it == 'b' }
        val r6 = CFUN("test", g)
        assertNotEquals(r1, r6)
    }

    @Test
    fun testCFUNCopyAndDestructuring() {
        val f: (Char) -> Boolean = { it == 'x' }
        val r = CFUN("test", f)
        val (asString, func) = r
        assertEquals("test", asString)
        assertSame(f, func)
    }

    @Test
    fun testCFUNPropertyAccess() {
        val f: (Char) -> Boolean = { it == 'z' }
        val r = CFUN("myPattern", f)
        // Access properties directly to cover getAsString() and getF() getters
        assertEquals("myPattern", r.asString)
        assertSame(f, r.f)
        assertTrue(r.f('z'))
        assertFalse(r.f('a'))
    }

    @Test
    fun testALT() {
        val a = CFUN("a") { it == 'a' }
        val b = CFUN("b") { it == 'b' }
        val alt = ALT(a, b)
        assertEquals("(a) | (b)", alt.toString())
        assertFalse(alt.nullable())
        assertTrue(ALT(ONE, a).nullable())
        assertTrue(ALT(a, ONE).nullable())

        assertEquals(ALT(ONE, ZERO), alt.der('a'))
        assertEquals(ALT(ZERO, ONE), alt.der('b'))

        assertEquals(Pair(a, Right(Empty)), ALT(ZERO, a).simp().let { it.first to it.second(Empty) })
        assertEquals(Pair(a, Left(Empty)), ALT(a, ZERO).simp().let { it.first to it.second(Empty) })

        val (r1, f1) = ALT(ZERO, a).simp()
        assertEquals(a, r1)
        assertEquals(Right(Empty), f1(Empty))

        val (r2, f2) = ALT(a, ZERO).simp()
        assertEquals(a, r2)
        assertEquals(Left(Empty), f2(Empty))

        val (r3, f3) = ALT(a, a).simp()
        assertEquals(a, r3)
        assertEquals(Left(Empty), f3(Empty))

        val (r4, f4) = ALT(a, b).simp()
        assertTrue(r4 is ALT)
        assertEquals(Left(Empty), f4(Left(Empty)))
        assertEquals(Right(Empty), f4(Right(Empty)))
        assertThrows(IllegalArgumentException::class.java) { f4(Empty) }

        assertEquals(Left(Empty), ALT(ONE, a).mkeps())
        assertEquals(Right(Empty), ALT(a, ONE).mkeps())

        val charAlt = ALT(CHAR('a'), CHAR('b'))
        val loweredAlt = charAlt.toCharFunctionFormat()
        assertTrue(loweredAlt is ALT)
        assertTrue((loweredAlt as ALT).r1 is CFUN)
        assertTrue(loweredAlt.r2 is CFUN)
    }

    @Test
    fun testSEQ() {
        val a = CFUN("a") { it == 'a' }
        val b = CFUN("b") { it == 'b' }
        val seq = SEQ(a, b)
        assertEquals("(a) ~ (b)", seq.toString())
        assertFalse(seq.nullable())
        assertTrue(SEQ(ONE, ONE).nullable())
        assertFalse(SEQ(ONE, a).nullable())

        // !r1.nullable()
        assertEquals(SEQ(ONE, b), seq.der('a'))

        // r1.nullable()
        val seqNull = SEQ(ONE, b)
        assertEquals(ALT(SEQ(ZERO, b), ONE), seqNull.der('b'))

        // simp branches
        assertEquals(ZERO, SEQ(ZERO, a).simp().first)
        assertThrows(Exception::class.java) { SEQ(ZERO, a).simp().second(Empty) }

        assertEquals(ZERO, SEQ(a, ZERO).simp().first)

        val (r1, f1) = SEQ(ONE, a).simp()
        assertEquals(a, r1)
        assertEquals(Seq(Empty, Empty), f1(Empty))

        val (r2, f2) = SEQ(a, ONE).simp()
        assertEquals(a, r2)
        assertEquals(Seq(Empty, Empty), f2(Empty))

        val (r3, f3) = SEQ(a, b).simp()
        assertTrue(r3 is SEQ)
        assertEquals(Seq(Empty, Empty), f3(Seq(Empty, Empty)))
        assertThrows(IllegalArgumentException::class.java) { f3(Empty) }

        assertEquals(Seq(Empty, Empty), SEQ(ONE, ONE).mkeps())

        val charSeq = SEQ(CHAR('a'), CHAR('b'))
        val loweredSeq = charSeq.toCharFunctionFormat()
        assertTrue(loweredSeq is SEQ)
        assertTrue((loweredSeq as SEQ).r1 is CFUN)
    }

    @Test
    fun testSTAR() {
        val a = CFUN("a") { it == 'a' }
        val star = STAR(a)
        assertEquals("(a)*", star.toString())
        assertTrue(star.nullable())
        assertEquals(SEQ(ONE, star), star.der('a'))

        val (rs, rect) = star.simp()
        assertEquals(star, rs)
        assertEquals(Empty, rect(Empty))
        assertEquals(Stars(emptyList()), star.mkeps())
        assertTrue(star.toCharFunctionFormat() is STAR)
    }

    @Test
    fun testPLUS() {
        val a = CFUN("a") { it == 'a' }
        val plus = PLUS(a)
        assertEquals("(a)+", plus.toString())
        assertFalse(plus.nullable())
        assertTrue(PLUS(ONE).nullable())
        assertEquals(SEQ(ONE, STAR(a)), plus.der('a'))

        val (rs, rect) = plus.simp()
        assertEquals(plus, rs)
        assertEquals(Empty, rect(Empty))
        assertEquals(Plus(listOf(Empty)), PLUS(ONE).mkeps())
        assertTrue(plus.toCharFunctionFormat() is PLUS)
    }

    @Test
    fun testRECD() {
        val a = CFUN("a") { it == 'a' }
        val recd = RECD("tag", a)
        assertEquals("(tag:a)", recd.toString())
        assertFalse(recd.nullable())
        assertTrue(RECD("tag", ONE).nullable())
        assertEquals(ONE, recd.der('a'))

        val (rs, rect) = recd.simp()
        assertEquals(recd, rs)
        assertEquals(Empty, rect(Empty))
        assertEquals(Rec("tag", Empty), RECD("tag", ONE).mkeps())
        assertTrue(recd.toCharFunctionFormat() is RECD)
    }

    @Test
    fun testRegexConveniences() {
        val a = CHAR('a')
        val b = CHAR('b')

        // X
        assertTrue(a X b is ALT)
        assertTrue(a X "b" is ALT)
        assertTrue("a" X b is ALT)
        assertTrue("a" X "b" is ALT)

        // F
        assertTrue(a F b is SEQ)
        assertTrue(a F "b" is SEQ)
        assertTrue("a" F b is SEQ)
        assertTrue("a" F "b" is SEQ)

        // S
        assertTrue(a.S() is STAR)
        assertTrue("a".S() is STAR)

        // P
        assertTrue(a.P() is PLUS)
        assertTrue("a".P() is PLUS)

        // T
        assertTrue("tag" T a is RECD)
        assertTrue("tag" T "a" is RECD)

        // toRegex
        assertEquals(SEQ(CHAR('a'), CHAR('b')), "ab".toRegex())
        assertEquals(ONE, "".toRegex())
    }

    @Test
    fun testRectification() {
        val rRect = RectificationFunctions.recd(RectificationFunctions.id)
        assertEquals(Rec("x", Empty), rRect(Rec("x", Empty)))
        assertThrows(IllegalArgumentException::class.java) { rRect(Empty) }

        // alt
        val fAlt = RectificationFunctions.alt(RectificationFunctions.id, RectificationFunctions.id)
        assertEquals(Left(Empty), fAlt(Left(Empty)))
        assertEquals(Right(Empty), fAlt(Right(Empty)))
        assertThrows(IllegalArgumentException::class.java) { fAlt(Empty) }

        // seq
        val fSeq = RectificationFunctions.seq(RectificationFunctions.id, RectificationFunctions.id)
        assertEquals(Seq(Empty, Empty), fSeq(Seq(Empty, Empty)))
        assertThrows(IllegalArgumentException::class.java) { fSeq(Empty) }
    }

    @Test
    fun testRectificationRight() {
        val fRight = RectificationFunctions.right(RectificationFunctions.id)
        assertEquals(Right(Empty), fRight(Empty))
        assertEquals(Right(Chr('a')), fRight(Chr('a')))
    }

    @Test
    fun testRectificationLeft() {
        val fLeft = RectificationFunctions.left(RectificationFunctions.id)
        assertEquals(Left(Empty), fLeft(Empty))
        assertEquals(Left(Chr('a')), fLeft(Chr('a')))
    }

    @Test
    fun testRectificationSeqEmpty1() {
        val f = RectificationFunctions.seqEmpty1(RectificationFunctions.id, RectificationFunctions.id)
        assertEquals(Seq(Empty, Chr('a')), f(Chr('a')))
    }

    @Test
    fun testRectificationSeqEmpty2() {
        val f = RectificationFunctions.seqEmpty2(RectificationFunctions.id, RectificationFunctions.id)
        assertEquals(Seq(Chr('a'), Empty), f(Chr('a')))
    }

    @Test
    fun testRectificationError() {
        assertThrows(Exception::class.java) { RectificationFunctions.ERROR(Empty) }
    }

    @Test
    fun testRectificationWithNonIdentityInnerFunctions() {
        // alt with wrapping inner functions
        val fAlt = RectificationFunctions.alt(
            { v -> Seq(v, Empty) },
            { v -> Seq(Empty, v) },
        )
        assertEquals(Left(Seq(Chr('a'), Empty)), fAlt(Left(Chr('a'))))
        assertEquals(Right(Seq(Empty, Chr('b'))), fAlt(Right(Chr('b'))))

        // seq with wrapping inner functions
        val fSeq = RectificationFunctions.seq(
            { v -> Left(v) },
            { v -> Right(v) },
        )
        assertEquals(Seq(Left(Chr('a')), Right(Chr('b'))), fSeq(Seq(Chr('a'), Chr('b'))))

        // recd with wrapping inner function
        val fRecd = RectificationFunctions.recd { v -> Left(v) }
        assertEquals(Rec("tag", Left(Empty)), fRecd(Rec("tag", Empty)))

        // right with wrapping inner function
        val fRight = RectificationFunctions.right { v -> Left(v) }
        assertEquals(Right(Left(Empty)), fRight(Empty))

        // left with wrapping inner function
        val fLeft = RectificationFunctions.left { v -> Right(v) }
        assertEquals(Left(Right(Empty)), fLeft(Empty))

        // seqEmpty1 with wrapping inner functions
        val fSeqE1 = RectificationFunctions.seqEmpty1(
            { v -> Left(v) },
            { v -> Right(v) },
        )
        assertEquals(Seq(Left(Empty), Right(Chr('x'))), fSeqE1(Chr('x')))

        // seqEmpty2 with wrapping inner functions
        val fSeqE2 = RectificationFunctions.seqEmpty2(
            { v -> Left(v) },
            { v -> Right(v) },
        )
        assertEquals(Seq(Left(Chr('x')), Right(Empty)), fSeqE2(Chr('x')))
    }

    @Test
    fun testCharlist2rexp() {
        // Empty list
        assertEquals(ONE, charlist2rexp(emptyList()))
        // Single char
        assertEquals(CHAR('a'), charlist2rexp(listOf('a')))
        // Multiple chars - recursive SEQ
        val result = charlist2rexp(listOf('a', 'b', 'c'))
        assertTrue(result is SEQ)
        assertEquals(CHAR('a'), (result as SEQ).r1)
        assertTrue(result.r2 is SEQ)
    }

    @Test
    fun testStringToRegexSingleChar() {
        assertEquals(CHAR('z'), "z".toRegex())
    }

    @Test
    fun testStringXString() {
        val result = "a" X "b"
        assertTrue(result is ALT)
    }

    @Test
    fun testStringFString() {
        val result = "a" F "b"
        assertTrue(result is SEQ)
    }

    @Test
    fun testStringTString() {
        val result = "tag" T "a"
        assertTrue(result is RECD)
        assertEquals("tag", result.x)
    }
}
