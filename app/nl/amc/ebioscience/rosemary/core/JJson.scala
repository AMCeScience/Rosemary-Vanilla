/*
 * Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
 * 
 * This program is semi-free software: you can redistribute it and/or modify it
 * under the terms of the Rosemary license. You may obtain a copy of this
 * license at:
 * 
 * https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * You should have received a copy of the Rosemary license
 * along with this program. If not, 
 * see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
 * 
 *        Project: https://github.com/AMCeScience/Rosemary-Vanilla
 *        AMC eScience Website: http://www.ebioscience.amc.nl/
 */
package nl.amc.ebioscience.rosemary.core

import org.json4s._
import play.api.libs.json._

object JJson {

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
