package nl.amc.ebioscience.rosemary.controllers

import play.api.libs.json._
import scala.language.implicitConversions

/**
 * With an enumeration like...
 * 
 * <pre>object Color extends Enumeration{...}</pre>
 * 
 * Create a format (or reads or writes) like this:
 * 
 * <pre>val fmt = EnumJson.enumFormat(Color);</pre>
 * 
 * The implementation: https://gist.github.com/agile-jordi/6809100
 */
object EnumJson {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException =>
            JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not contain '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }
}
