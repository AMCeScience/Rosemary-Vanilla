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
    def toValunit = t._1 -> Valunit(t._2)
  }
  implicit class ListTupleToListValunit(ts: List[(String, String)]) {
    def toValunit = ts.map(_.toValunit)
  }
}
