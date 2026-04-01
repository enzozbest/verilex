import tokenizer.SMLTokeniser
import kotlin.io.path.Path
import kotlin.io.path.readText

fun main() {
    val input = """
    
  val  z:: _= 1
exception  u of ly  kiy and  k9 = fT exception bH8
;h        
    """.trimIndent()

    val input_file = Path("/home/enzozbest/Repositories/polyfuzz/campaigns2/campaign_000/diffcomp_input/id:000111,src:000002,time:292,execs:953,op:havoc,rep:7.sml")
    val verilexTokens = SMLTokeniser.tokenise(input_file.readText()).withoutTrivia()
    val verilexOutput = verilexTokens.toCompactString()
    println("Verilex Tokens: $verilexOutput")
}
