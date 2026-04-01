package tokenizer

import lexer.Verilex

/**
 * Tokeniser for Standard ML source code.
 *
 * This tokeniser uses the Verilex derivative-based lexer engine with the SML token specification defined in [SMLLexerSpec].
 * It produces a [TokenSequence] of [Token] objects with full position tracking.
 * Important: the tokeniser follows POSIX lexing rules:
 *   1. Maximal munch (longest match) semantics
 *   2. Reserved word priority over identifiers
 *
 * Characters not covered by any token rule (e.g. lone `"`, `'`, `.`) are emitted as ERROR tokens
 * via recovery rather than crashing the lexer. Adding such characters to the regex-level error
 * catch-all would create ambiguity with multi-character patterns (strings, type variables, ellipsis)
 * and cause Brzozowski derivative explosion, so recovery must happen here instead.
 */
object SMLTokeniser : Tokeniser<TokenSequence>() {
    override fun tokenise(source: String): TokenSequence {
        if (source.isEmpty()) return TokenSequence(emptyList())

        return try {
            val rawTokens = Verilex.lex(SMLLexerSpec.lexer, source)
            TokenSequence(buildTokenList(rawTokens))
        } catch (_: Exception) {
            tokeniseWithRecovery(source)
        }
    }

    /**
     * Lex the longest valid prefix, emit ERROR for the unmatchable character, repeat.
     */
    private fun tokeniseWithRecovery(source: String): TokenSequence {
        val tokens = mutableListOf<Token>()
        var pos = 0

        while (pos < source.length) {
            val remaining = source.substring(pos)

            try {
                val rawTokens = Verilex.lex(SMLLexerSpec.lexer, remaining)
                var localPos = pos
                for ((tag, lexeme) in rawTokens) {
                    tokens.add(Token.fromPair(tag to lexeme, localPos))
                    localPos += lexeme.length
                }
                break
            } catch (_: Exception) {
                val validLen = findValidPrefixLength(remaining)

                if (validLen > 0) {
                    val prefixTokens = Verilex.lex(SMLLexerSpec.lexer, remaining.substring(0, validLen))
                    var localPos = pos
                    for ((tag, lexeme) in prefixTokens) {
                        tokens.add(Token.fromPair(tag to lexeme, localPos))
                        localPos += lexeme.length
                    }
                }

                tokens.add(Token.fromPair("ERROR" to remaining[validLen].toString(), pos + validLen))
                pos += validLen + 1
            }
        }

        return TokenSequence(tokens)
    }

    /**
     * Binary search for the longest prefix of [source] that Verilex can tokenise.
     * Invariant: canTokenise("") is always true; canTokenise(source) is false (we are in recovery).
     * Returns the length of the longest tokenisable prefix (0 if even the first character fails).
     */
    private fun findValidPrefixLength(source: String): Int {
        var low = 0
        var high = source.length
        while (high - low > 1) {
            val mid = low + (high - low) / 2
            if (canTokenise(source.substring(0, mid))) {
                low = mid
            } else {
                high = mid
            }
        }
        return low
    }
}
