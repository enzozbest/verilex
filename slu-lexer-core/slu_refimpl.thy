theory slu_refimpl 
  imports "Posix-Lexing.Lexer3"
begin

export_code lexer in Scala
  module_name "slu_refimpl"
  file_prefix "generated/"

end                                                                          
