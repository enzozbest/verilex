Verilex
=======
A Kotlin lexer for Standard ML backed by a **formally verified** POSIX lexing
algorithm extracted from Isabelle/HOL.

Verilex implements derivative-based lexing as described by Sulzmann and Lu (2014)
and formally proved correct by Ausaf, Dyckhoff, and Urban (2016). The primary
lexer is Isabelle-extracted Scala code (`SLULexer`) that has been mechanically
verified to implement POSIX semantics: longest match first, with ties broken by
rule declaration order. A Kotlin re-implementation (`Verilex`) serves as a
fallback and is used for error recovery.

## Quick start

```bash
./gradlew test              # run the test suite
./gradlew jacocoTestReport  # generate coverage report
```

Requires **JDK 25** and a **Scala 3 runtime** (both handled by the Gradle wrapper
and declared dependencies).

## Architecture

Verilex has two lexer backends:

1. **Verified lexer** (`formallex/FormalLexer`) — Isabelle-extracted Scala code
   (`SLULexer`) from the Archive of Formal Proofs. This is the primary backend.
   It operates on Isabelle-native types (`SLULexer.char`, `SLULexer.rexp`,
   `SLULexer.vala`) so a conversion layer (`KotlinScalaConverter`) translates
   between Kotlin and Isabelle representations.

2. **Kotlin lexer** (`lexer/Verilex`) — A pure-Kotlin re-implementation of the
   same algorithm used as a fallback when the verified lexer is unavailable and
   for error recovery (tokenising valid prefixes around unmatchable characters).

The `SMLTokeniser` tries the verified lexer first; if it fails, it falls back to
the Kotlin lexer with character-by-character recovery.

## How it works

Regular expressions are represented as a sealed Kotlin class hierarchy
(`RegularExpression`). A small DSL makes composition readable:

| DSL       | Meaning                               |
|-----------|---------------------------------------|
| `a F b`   | Sequence — *a* followed by *b*        |
| `a X b`   | Choice — *a* or *b*                   |
| `r.S()`   | Kleene star — zero or more of *r*     |
| `r.P()`   | Plus — one or more of *r*             |
| `"t" T r` | Tag — label the sub-expression as *t* |

The Kotlin lexer (`Verilex.lex`) walks the input one character at a time,
computing Brzozowski derivatives of the combined regex while an `Injector` builds
a structured *value tree* that records which sub-expression consumed each
character. When the input is exhausted the value tree is flattened into a list of
`(tag, lexeme)` pairs.

The verified lexer (`SLULexer.slexer`) performs the same algorithm but with
Isabelle-extracted simplification rules that are proven correct. It takes the
regex in Isabelle's `rexp` type and the input as a list of Isabelle `char`
(8 booleans encoding a byte). The result is an Isabelle `vala` parse tree, which
`KotlinScalaConverter` translates back to Verilex's `Value` type.

### Example

```kotlin
fun main() {
    // Operators: + - * /
    val op = "op" T ("+" X "-" X "*" X "/")

    // Identifier: one or more lowercase letters
    val letters = RANGE(('a'..'z').toSet())
    val id = "id" T (letters F letters.S())

    // Compile and lex
    val regex = (op X id).S().toCharFunctionFormat()
    val tokens = Verilex.lex(regex, "sum+a-b/c")

    println(tokens)
    // [(id, sum), (op, +), (id, a), (op, -), (id, b), (op, /), (id, c)]
}
```

## Project structure

```
src/
├── main/kotlin/
│   ├── formallex/
│   │   ├── FormalLexer.kt           Entry point for the verified lexer
│   │   └── KotlinScalaConverter.kt  Bidirectional type conversion (Kotlin ↔ Isabelle)
│   ├── lexer/
│   │   ├── Verilex.kt               Kotlin lexer — derivative loop + token extraction
│   │   └── Injector.kt              Character injection into the value tree
│   ├── rexp/
│   │   ├── RegularExpression.kt     Sealed hierarchy (ALT, SEQ, STAR, PLUS, …)
│   │   ├── RegexConveniences.kt     DSL helpers (F, X, S, P, T)
│   │   └── RectificationFunctions.kt  Algebraic simplification of values
│   ├── tokenizer/
│   │   ├── Tokeniser.kt             Base tokeniser
│   │   ├── SMLTokeniser.kt          SML tokeniser (verified → Kotlin fallback)
│   │   ├── SMLLexerSpec.kt          SML token rules + combined regex
│   │   ├── SMLRegexes.kt            SML-specific patterns (strings, comments, …)
│   │   ├── SMLToken.kt              Token type definitions
│   │   ├── TokeniserConveniences.kt Utility extensions
│   │   └── TokenisationError.kt     Error type
│   └── value/
│       └── Value.kt                 Value algebra for structured match results
├── main/scala/
│   ├── SLULexer.scala               Isabelle-extracted verified lexer (DO NOT EDIT)
│   └── verified-lexer.jar           Pre-compiled JAR of SLULexer.scala
└── test/kotlin/                     Mirrors the source tree; 100 % coverage
```

## Algorithm

Both backends implement the same algorithm:

1. **Build** — Compose your token rules with the DSL. For the Kotlin backend,
   call `toCharFunctionFormat()` to compile character classes into predicate
   form. For the verified backend, `KotlinScalaConverter.toRexp()` translates
   to Isabelle's `rexp` type.
2. **Derive** — For each input character *c*, compute the Brzozowski derivative
   of the current regex with respect to *c*. This yields a new regex that
   matches the *remainder* of the input.
3. **Simplify** — The verified lexer applies proven simplification rules to
   keep the derivative compact, pairing each simplification with a
   rectification function that reconstructs the original match structure.
4. **Inject** — Build a value tree by injecting *c* into the derivative's
   structure, recording which branch of the regex consumed it.
5. **Flatten** — Once the full input is consumed, call `env()` on the value tree
   to extract `List<Pair<String, String>>` — each pair is `(tag, lexeme)`.

### Kotlin ↔ Isabelle type conversion

The verified lexer operates on Isabelle-native types that differ from Kotlin's:

| Kotlin type             | Isabelle type           | Notes                                      |
|-------------------------|-------------------------|--------------------------------------------|
| `Char`                  | `SLULexer.char`         | 8 booleans encoding a byte (LSB-first)     |
| `String`                | `List[SLULexer.char]`   | Scala immutable list of Isabelle chars      |
| `RegularExpression`     | `SLULexer.rexp[char]`   | `CHAR`/`RANGE` map to `Atom`/`Charset`     |
| `Value`                 | `SLULexer.vala[char]`   | Parse tree returned by the verified lexer  |
| `Int`                   | `SLULexer.nat`          | Peano encoding (unary)                     |

`KotlinScalaConverter` handles all conversions. Scala's `implicit` type-class
parameters (e.g. `equal[char]`) must be passed explicitly from Kotlin since
implicits do not cross the language boundary.

### References

- Sulzmann, M. & Lu, K. (2014). *POSIX Regular Expression Parsing with
  Derivatives.* FLOPS 2014.
- Ausaf, F., Dyckhoff, R. & Urban, C. (2016). *POSIX Lexing with Derivatives of
  Regular Expressions.* Archive of Formal Proofs.
  https://www.isa-afp.org/entries/Posix-Lexing.html

## SML tokeniser

Verilex ships with a full Standard ML tokeniser (`SMLTokeniser`) that handles
reserved words, symbolic and alphanumeric identifiers, integer and real literals,
string literals with escape sequences, and comments. Whitespace and comments are
retained as *trivia* tokens, so the tokeniser is lossless; call
`.withoutTrivia()` on the result to discard them.

The tokeniser first attempts to lex the input with the verified lexer. If that
fails (e.g. due to a regex the Isabelle extraction does not yet handle), it falls
back to the Kotlin lexer. For inputs containing characters not covered by any
SML token rule (e.g. lone `"`, `'`, `.`), the tokeniser enters a recovery mode
that emits `ERROR` tokens for unmatchable characters while preserving valid
tokens around them.

```kotlin
val result = SMLTokeniser.tokenise("val x = 42")
println(result.withoutTrivia())
```

## Testing

The test suite lives under `src/test/kotlin/` and mirrors the source layout.

```bash
./gradlew test                # run the test suite
./gradlew jacocoTestReport    # generate HTML + CSV coverage report
```

Coverage reports are written to `build/reports/jacoco/test/html/`.

## Build details

| Tool          | Version |
|---------------|---------|
| Kotlin        | 2.3.0   |
| JDK           | 25      |
| Gradle        | 9.2     |
| Scala 3       | 3.3.4   |
| JaCoCo        | (Gradle default) |
| JUnit 5       | 5.10.1  |

Runtime dependencies: Kotlin stdlib, Scala 3 library, and the pre-compiled
verified lexer JAR (`verified-lexer.jar`).

## Limitations

- The verified lexer is extracted from Isabelle with `sys.error("undefined")` for
  unreachable rectification functions. These must be wrapped in lambdas to avoid
  eager evaluation (see `simp_Times` in `SLULexer.scala`).
- `CFUN` (character-predicate) regex nodes cannot be translated to the Isabelle
  representation. The SML lexer spec avoids `CFUN` by using `CHAR`/`RANGE`
  instead, but custom lexer specs must do the same.
- Performance has not been optimised (the focus is on correctness).
- The Scala interop requires explicit passing of Isabelle type-class instances
  (e.g. `equal[char]`) since Scala implicits are not visible to Kotlin.

