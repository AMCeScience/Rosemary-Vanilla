package nl.amc.ebioscience.rosemary.core

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.json4s._
import play.api.libs.json._
import java.math.BigInteger

object JJson extends ObjectMapper {
  registerModule(DefaultScalaModule)

  /**
   * Converts Jackson JSONs to Play JSONs
   */
  def toValue(value: JValue): JsValue = {
    value match {
      case v: JString  => toString(v)
      case v: JDouble  => toNumber(v)
      case v: JDecimal => toNumber(v)
      case v: JInt     => toNumber(v)
      case v: JBool    => toBoolean(v)
      case v: JObject  => toObject(v)
      case v: JArray   => toArray(v)
      case _           => JsNull
    }
  }

  private def toObject(o: JObject): JsObject = new JsObject(o.obj.map(field => (field._1, toValue(field._2))).toMap)
  private def toString(s: JString): JsString = new JsString(s.values)
  private def toNumber(d: JDouble): JsNumber = new JsNumber(d.values)
  private def toNumber(d: JDecimal): JsNumber = new JsNumber(d.values)
  private def toNumber(i: JInt): JsNumber = new JsNumber(i.values.toInt)
  private def toBoolean(b: JBool): JsBoolean = new JsBoolean(b.values)
  private def toArray(a: JArray): JsArray = new JsArray(a.arr.map(toValue(_)))
}
