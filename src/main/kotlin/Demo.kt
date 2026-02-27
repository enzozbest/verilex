import tokenizer.SMLTokeniser

fun main() {
    val input = """
    
  val  z:: _= 1
exception  u of ly  kiy and  k9 = fT exception bH8
;h        
    """.trimIndent()
    val verilexTokens = SMLTokeniser.tokenise(input).withoutTrivia()
    val verilexOutput = verilexTokens.toCompactString()
    println("Verilex Tokens: $verilexOutput")
}
