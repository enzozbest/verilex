package formallex

import formallex.KotlinScalaConverter.toIsabelleList
import formallex.KotlinScalaConverter.toKotlinValue
import formallex.KotlinScalaConverter.toRexp
import rexp.RegularExpression
import scala.Some
import value.Value
import verified.SLULexer

/**
 * Entry point for invoking the formally verified POSIX lexer from Kotlin.
 *
 * This object wraps [SLULexer.slexer] — a lexer extracted from an Isabelle/HOL formalisation
 * of Sulzmann and Lu's 2014 POSIX lexing algorithm (with Brzozowski derivative-based
 * simplification). The formalisation was mechanised by Ausaf, Dyckhoff, and Urban and is
 * available in the Archive of Formal Proofs (Posix-Lexing entry).
 *
 * Because the extracted code is Scala and uses Isabelle-specific types, this object acts as a
 * thin adapter: it converts Verilex's Kotlin [RegularExpression] and [String] inputs into the
 * Isabelle types via [KotlinScalaConverter], calls the verified lexer, and converts the
 * resulting Isabelle parse tree ([SLULexer.vala]) back into a Verilex [Value].
 *
 * The verified lexer guarantees POSIX semantics — longest match with left-priority for
 * alternatives — by construction (the proof covers correctness of both the matching and
 * the value construction). This means any result returned by [verifiedLex] is the unique
 * POSIX parse tree for the given regex and input, or `null` if no match exists.
 *
 * @see KotlinScalaConverter for the type-conversion layer between Kotlin and Isabelle types.
 * @see SLULexer for the auto-generated Scala code from the formal proof.
 */
object FormalLexer {

    /**
     * The Isabelle-extracted equality type-class instance for [SLULexer.char].
     *
     * Scala implicits do not cross the Kotlin/Scala interop boundary, so this instance must be
     * passed explicitly to [SLULexer.slexer]. It is obtained from the companion object of the
     * [SLULexer.equal] trait via its JVM-mangled name (`SLULexer$u002Eequal_char`, where
     * `u002E` is the Scala 3 encoding of `.` in identifier names).
     */
    private val equalChar: SLULexer.equal<SLULexer.char> =
        SLULexer.equal.`SLULexer$u002Eequal_char`()

    /**
     * Runs the verified POSIX lexer on the given regular expression and input string.
     *
     * @param r The regular expression to match against, expressed using Verilex's
     *   [RegularExpression] sealed hierarchy. It will be converted to the Isabelle [SLULexer.rexp]
     *   type internally via [KotlinScalaConverter.toRexp].
     * @param input The source string to lex. It will be converted to a
     *   `scala.collection.immutable.List<SLULexer.char>` via [KotlinScalaConverter.toIsabelleList].
     * @return The POSIX parse tree as a Verilex [Value] if the input matches the regex, or `null`
     *   if no match exists. The returned value encodes exactly how the input was decomposed
     *   according to POSIX rules (longest match, left-priority for alternatives).
     */
    @Suppress("UNCHECKED_CAST")
    fun verifiedLex(r: RegularExpression, input: String): Value? {
        val result = SLULexer.slexer(r.toRexp(), input.toIsabelleList(), equalChar)
        return when (result) {
            is Some<*> -> (result.value() as SLULexer.vala<SLULexer.char>).toKotlinValue()
            else -> null
        }
    }
}