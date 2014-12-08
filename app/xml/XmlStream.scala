package dst.lib.xml

import scala.xml._
import scala.xml.pull._
import scala.util.{Try, Success, Failure}
import java.nio.file.Path
import scala.io.Source

object XMLStream {
  import scala.xml.pull._
 
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

  private def matchEvent(ev: XMLEvent): String = {
    ev match {
      case EvElemStart(_, "text", _, _) => {
        backToXml(ev)
      }
      case EvElemStart(_, _, _, _) => { backToXml(ev) }
      case EvText(text) => {
        text
      } 
      case EvElemEnd(_, "text") => {
        backToXml(ev)
      }
      case EvElemEnd(_, _) => { backToXml(ev) }
      case _ => ""
    }
  }

  def apply(xmlPath: Path, label: String) = {
    val (startTag, endTag) = (s"<$label>", s"</$label>")
    // Get an iterator to the XML with preceding non-valid characters removed.
    val xmlIterator = scala.io.Source.fromFile(xmlPath.normalize().toString, "utf-8").dropWhile(_ != '<')

    val xmlSource = scala.io.Source.fromIterable(xmlIterator.toIterable)
    val offers = new XMLEventReader(xmlSource) map matchEvent

    Stream.continually {
      offers.dropWhile(_ != startTag).takeWhile(_ != endTag).mkString + endTag match {
        case `endTag` => Stream.empty
        case offerString => {
          XML.loadString(offerString)
        }
      }
    } takeWhile (_ != Stream.empty)
  }
}
