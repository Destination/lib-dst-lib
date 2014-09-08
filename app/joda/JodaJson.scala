package dst.lib.joda

import org.joda.time._
import org.joda.time.DateTimeZone.UTC
import org.joda.time.format.ISODateTimeFormat

import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError

object JodaJson {
  val dateFormat = ISODateTimeFormat.dateTimeNoMillis.withOffsetParsed

  implicit object DateTimeReads extends Reads[DateTime] {
    def reads(json: JsValue) = json match {
      case JsString(s) => {
        try {
          JsSuccess(dateFormat.parseDateTime(s))
        } catch {
          case e: NoSuchElementException => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.tokentype.format"))))
        }
      }
      case JsNumber(n) => JsSuccess(new DateTime(n.toLong * 1000, UTC))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
    }
  }
  implicit object DateTimeWrites extends Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsString(dateFormat.print(d))
  }

  implicit object DurationReads extends Reads[Duration] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(new Duration(n.toLong * 1000))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }
  implicit object DurationWrites extends Writes[Duration] {
    def writes(d: Duration): JsValue = JsNumber(d.getStandardSeconds)
  }
}
