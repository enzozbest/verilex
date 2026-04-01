package formallex

import formallex.KotlinScalaConverter.toIsabelleList
import formallex.KotlinScalaConverter.toKotlinValue
import formallex.KotlinScalaConverter.toRexp
import rexp.RegularExpression
import scala.Some
import value.Value
import verified.SLULexer

object FormalLexer {
    private val equalChar: SLULexer.equal<SLULexer.char> =
        SLULexer.equal.`SLULexer$u002Eequal_char`()

    @Suppress("UNCHECKED_CAST")
    fun verifiedLex(r: RegularExpression, input: String): Value? {
        val result = SLULexer.slexer(r.toRexp(), input.toIsabelleList(), equalChar)
        return when (result) {
            is Some<*> -> (result.value() as SLULexer.vala<SLULexer.char>).toKotlinValue()
            else -> null
        }
    }
}