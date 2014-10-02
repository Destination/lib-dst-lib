package dst.lib.xml

import scala.xml._
import scala.xml.pull._
import scala.util.{Try, Success, Failure}
import java.nio.file.Path
import scala.io.Source

object XMLStream {
  import scala.xml.pull._

  private val interLink = """\[\[(.*)\]\]""".r
  private var readingText = false
 
  private def backToXml(ev: XMLEvent) = {
    ev match {
      case EvElemStart(pre, label, attrs, scope) => {
        "<" + label + attrsToString(attrs) + ">"
      }
      case EvElemEnd(pre, label) => {
        "</" + label + ">"
      }
      case _ => ""
    }
  }
 
  private def attrsToString(attrs:MetaData) = {
    attrs.length match {
      case 0 => ""
      case _ => attrs.map( (m:MetaData) => " " + m.key + "='" + m.value +"'" ).reduceLeft(_+_)
    }
  }
 
  private def filterText(text: String) = {
    val matches = interLink.findAllIn(text)
    if (matches.hasNext) matches.reduceLeft(_+_) else ""
  }

  private def matchEvent(ev: XMLEvent): String = {
    ev match {
      case EvElemStart(_, "text", _, _) => { 
        readingText = true
        backToXml(ev)
      }
      case EvElemStart(_, _, _, _) => { backToXml(ev) }
      case EvText(text) => {
        if (readingText) filterText(text) else text
      } 
      case EvElemEnd(_, "text") => {
        readingText = false
        backToXml(ev)
      }
      case EvElemEnd(_, _) => { backToXml(ev) }
      case _ => ""
    }
  }

  def apply(xmlPath: Path, label: String): Stream[Node] = {

    val (startTag, endTag) = (s"<$label>", s"</$label>")
    var offers = new XMLEventReader(scala.io.Source.fromFile(xmlPath.normalize().toString, "utf-8")) map matchEvent
  

    Stream.continually {
      XML.loadString(offers.dropWhile(_ != startTag).takeWhile(_ != endTag).mkString + endTag)
    }
  }
}