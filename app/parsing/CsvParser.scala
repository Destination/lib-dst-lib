package dst.lib.parsing

import scala.language.postfixOps
import scala.util.parsing.combinator.RegexParsers

trait CsvParser extends RegexParsers {
  override val skipWhitespace = false

  private def TextSource = s"[^${Quote}${Separator}\\r\\n]"

  def Separator   = ","
  def Quote       = "\""
  def DoubleQuote = "\"\"" ^^ ( _ => "\"")
  def Text        = TextSource.r
  def Spaces      = "[ \t]+".r
  def NewLine     = "\r\n" | "\n"

  def file: Parser[List[List[String]]]  = repsep(record, NewLine) <~ (NewLine?)

  def record: Parser[List[String]]      = repsep(field, Separator)
  def field: Parser[String]             = escaped|nonescaped

  def escaped: Parser[String]           = ((Spaces?) ~> Quote ~> ((Text|Separator|NewLine|DoubleQuote)*) <~ Quote <~ (Spaces?)) ^^ ( _.mkString(""))
  def nonescaped: Parser[String]        = (Text*) ^^ ( _.mkString(""))

  def parse(s: String) = parseAll(file, s) match {
    case Success(result, _) =>  scala.util.Success(result)
    case failure =>             scala.util.Failure(new Exception(failure.toString))
  }
  def parseLine(s: String) = parseAll(record, s) match {
    case Success(result, _) =>  scala.util.Success(result)
    case failure =>             scala.util.Failure(new Exception(failure.toString))
  }
}

object CsvParser extends CsvParser
