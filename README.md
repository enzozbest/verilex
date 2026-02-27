Verilex
=======
A Kotlin implementation of the formally verified lexing algorithm by Sulzmann and Lu (2014) for Standard ML.

Disclaimer: verilex itself is not formally verified, but it mirrors the algorithm closely. As such, no bugs are 
expected in the core lexing logic, but visual inspection by other developers is encouraged to confirm this.

Verilex implements derivative-based lexing as described by Sulzmann and Lu (2014)
and formally proved correct by Urban (2016). The algorithm guarantees POSIX
disambiguation with longest match first and ties broken by rule declaration order. This means
you get correct tokenisation by construction rather than by hand-tuned edge cases.

## Quick start

```bash
./gradlew test        # run the test suite
./gradlew coverage    # run tests + enforce 100 % line and branch coverage
```

Requires **JDK 23** (the Gradle wrapper handles everything else).

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

The lexer core (`Verilex.lex`) walks the input one character at a time, computing Brzozowski derivatives of the 
combined regex while an `Injector` builds a  structured *value tree* that records which sub-expression consumed 
each character. When the input is exhausted the value tree is flattened into a list of `(tag, lexeme)` pairs.

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
│   ├── lexer/
│   │   ├── Verilex.kt              Core lexer — derivative loop + token extraction
│   │   └── Injector.kt             Character injection into the value tree
│   ├── rexp/
│   │   ├── RegularExpression.kt    Sealed hierarchy (ALT, SEQ, STAR, PLUS, …)
│   │   ├── RegexConveniences.kt    DSL helpers (F, X, S, P, T)
│   │   └── RectificationFunctions.kt  Algebraic simplification of values
│   ├── tokenizer/
│   │   ├── Tokeniser.kt            Base tokeniser
│   │   ├── SMLTokeniser.kt         Standard ML tokeniser built on Verilex
│   │   ├── SMLLexerSpec.kt         SML token rules + combined regex
│   │   ├── SMLRegexes.kt           SML-specific patterns (strings, comments, …)
│   │   ├── SMLToken.kt             Token type definitions
│   │   ├── TokeniserConveniences.kt  Utility extensions
│   │   └── TokenisationError.kt    Error type
│   └── value/
│       └── Value.kt                Value algebra for structured match results
└── test/kotlin/                    Mirrors the source tree; 100 % coverage
```

## Algorithm

The key steps are:

1. **Build** — Compose your token rules with the DSL and call
   `toCharFunctionFormat()` to compile character classes into predicate form.
2. **Derive** — For each input character *c*, compute the Brzozowski derivative
   of the current regex with respect to *c*. This yields a new regex that
   matches the *remainder* of the input.
3. **Inject** — Simultaneously build a value tree by injecting *c* into the
   derivative's structure, recording which branch of the regex consumed it.
4. **Flatten** — Once the full input is consumed, call `env()` on the value tree
   to extract `List<Pair<String, String>>` — each pair is `(tag, lexeme)`.

Because derivatives and injection follow the algebraic laws of regular expressions, the algorithm is 
provably correct: it will always return the unique POSIX parse of the input.

### References

- Sulzmann, M. & Lu, K. (2014). *POSIX Regular Expression Parsing with
  Derivatives.* FLOPS 2014.
- Urban, C. (2016). *Derivatives of Regular Expressions and an Application to
  Lexing (formalisation in Isabelle/HOL).*

## SML tokeniser

Verilex ships with a full Standard ML tokeniser (`SMLTokeniser`) that  handles reserved words, symbolic and 
alphanumeric identifiers, integer and real literals, string literals with escape sequences, and comments. Whitespace
and comments are retained as *trivia* tokens, so the tokeniser is lossless; call `.withoutTrivia()` on the result to discard them.

```kotlin
val result = SMLTokeniser.tokenise("val x = 42")
println(result.withoutTrivia())
```

## Testing

The test suite lives under `src/test/kotlin/` and mirrors the source layout.
JaCoCo is configured to enforce **100 % line and branch coverage**:

```bash
./gradlew coverage   # test → report → verify
```

Individual tasks are also available:

```bash
./gradlew test                               # just run tests
./gradlew jacocoTestReport                   # generate HTML/XML report
./gradlew jacocoTestCoverageVerification     # check the 100 % threshold
```

Coverage reports are written to `build/reports/jacoco/`.

## Build details

| Tool    | Version |
|---------|---------|
| Kotlin  | 2.3.0   |
| JDK     | 23      |
| Gradle  | 8.13    |
| JaCoCo  | 0.8.11  |
| JUnit 5 | 5.10.1  |

The project has **no runtime dependencies** beyond the Kotlin standard library.

## Limitations

- Error reporting is minimal. If the regex cannot match the full input an
  exception is thrown from the derivative/injection pipeline rather than
  producing a diagnostic with source location.
- Performance has not been optimised (the focus is on correctness).

