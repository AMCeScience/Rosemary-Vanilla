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
package nl.amc.ebioscience.rosemary.models.core

import com.novus.salat.annotations._
import java.util.Date

@Salat
trait BaseEntity {
  val id: DefaultModelBase.Id
  val info: Info
}

case class Info(
    dict: Map[String, Valunit] = Map.empty,
    inheritedDict: Map[String, Valunit] = Map.empty,
    ascendents: List[Catname] = List(),
    created: Date = new Date(),
    var updated: Date = new Date()) {

  updated = new Date() // TODO: Fix in Post-Save hook of Salat?

  def addToDict(k: String, v: Valunit) = copy(dict = dict + (k -> v))
  def addToDict(d: Map[String, Valunit]) = copy(dict = dict ++ d)
}

case class Valunit(value: String, unit: Option[String] = None)
case class Catname(category: String, name: String)

object ValunitConvertors {
  import scala.language.implicitConversions

  implicit class TupleToValunit(t: (String, String)) {
    def toValunit = t._1 -> t._2.toValunit
  }

  implicit class MapToValunit(ms: Map[String, String]) {
    def toValunit = ms.mapValues(_.toValunit)
  }

  implicit class ListTupleToListValunit(ts: List[(String, String)]) {
    def toValunit = ts.map(_.toValunit)
  }

  implicit class StringToValunit(str: String) {
    def toValunit = Valunit(str)
  }
}
