package formallex

import rexp.ALT
import rexp.CFUN
import rexp.CHAR
import rexp.ONE
import rexp.PLUS
import rexp.RANGE
import rexp.RECD
import rexp.RegularExpression
import rexp.SEQ
import rexp.STAR
import rexp.ZERO
import value.Chr
import value.Empty
import value.Left
import value.Rec
import value.Right
import value.Seq
import value.Stars
import value.Value
import verified.SLULexer
import scala.jdk.javaapi.CollectionConverters

/**
 * Used to translate a [rexp.RegularExpression] into a [rexp[char]] for use within the auto-generated lexer from the formal proof.
 * This is necessary because that lexer (Scala) uses its own types and we must convert between them.
 */

object KotlinScalaConverter {

    private fun <T> List<T>.toScalaList(): scala.collection.immutable.List<T> =
        CollectionConverters.asScala(this).toList()

    private fun <T> scala.collection.immutable.List<T>.toKotlinList(): List<T> =
        CollectionConverters.asJava(this)

    fun Char.toIsabelleChar(): SLULexer.char =
        SLULexer.Char(
            this.code and 1 != 0,
            this.code and 2 != 0,
            this.code and 4 != 0,
            this.code and 8 != 0,
            this.code and 16 != 0,
            this.code and 32 != 0,
            this.code and 64 != 0,
            this.code and 128 != 0,
        )

    fun SLULexer.char.toKotlinChar(): Char {
        val c = this as SLULexer.Char
        var code = 0
        if (c.a()) code = code or 1
        if (c.b()) code = code or 2
        if (c.c()) code = code or 4
        if (c.d()) code = code or 8
        if (c.e()) code = code or 16
        if (c.f()) code = code or 32
        if (c.g()) code = code or 64
        if (c.h()) code = code or 128
        return code.toChar()
    }

    fun String.toIsabelleList(): scala.collection.immutable.List<SLULexer.char> =
        this.map { it.toIsabelleChar() }.toScalaList()

    fun Int.toIsabelleNat(): SLULexer.nat {
        var result: SLULexer.nat = SLULexer.zero_nat()
        repeat(this) { result = SLULexer.Suc(result) }
        return result
    }

    fun RegularExpression.toRexp(): SLULexer.rexp<SLULexer.char> = when (this) {
        is ZERO -> SLULexer.Zero<SLULexer.char>()
        is ONE -> SLULexer.One<SLULexer.char>()
        is CHAR -> SLULexer.Atom<SLULexer.char>(c.toIsabelleChar())
        is RANGE -> SLULexer.Charset<SLULexer.char>(SLULexer.seta<SLULexer.char>(cs.map { it.toIsabelleChar() }.toScalaList()))
        is ALT -> SLULexer.Plus<SLULexer.char>(r1.toRexp(), r2.toRexp())
        is SEQ -> SLULexer.Times<SLULexer.char>(r1.toRexp(), r2.toRexp())
        is STAR -> SLULexer.Star<SLULexer.char>(r.toRexp())
        is PLUS -> SLULexer.Times<SLULexer.char>(r.toRexp(), SLULexer.Star<SLULexer.char>(r.toRexp()))
        is RECD -> SLULexer.Rec<SLULexer.char>(x.toIsabelleList(), r.toRexp())
        is CFUN -> error("CFUN cannot be converted to Isabelle rexp")
    }

    @Suppress("UNCHECKED_CAST")
    fun SLULexer.vala<SLULexer.char>.toKotlinValue(): Value = when (this) {
        is SLULexer.Void -> Empty
        is SLULexer.Atm<*> -> Chr((this.a() as SLULexer.char).toKotlinChar())
        is SLULexer.Seq<*> -> Seq(
            (this.a() as SLULexer.vala<SLULexer.char>).toKotlinValue(),
            (this.b() as SLULexer.vala<SLULexer.char>).toKotlinValue(),
        )
        is SLULexer.Left<*> -> Left((this.a() as SLULexer.vala<SLULexer.char>).toKotlinValue())
        is SLULexer.Right<*> -> Right((this.a() as SLULexer.vala<SLULexer.char>).toKotlinValue())
        is SLULexer.Stars<*> -> Stars(
            (this.a() as scala.collection.immutable.List<SLULexer.vala<SLULexer.char>>)
                .toKotlinList().map { it.toKotlinValue() }
        )
        is SLULexer.Recv<*> -> Rec(
            (this.a() as scala.collection.immutable.List<SLULexer.char>)
                .toKotlinList().map { it.toKotlinChar() }.joinToString(""),
            (this.b() as SLULexer.vala<SLULexer.char>).toKotlinValue(),
        )
        else -> error("Unknown vala type: ${this::class}")
    }

}
