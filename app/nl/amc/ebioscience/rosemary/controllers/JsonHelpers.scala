package nl.amc.ebioscience.rosemary.controllers

import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.functional.syntax._
import org.bson.types.ObjectId
import play.api.data.validation.ValidationError
import scala.language.postfixOps

trait JsonHelpers {

  // Serialization for reading and writing BSON ObjectId
  implicit val objectIdFormat = new Format[ObjectId] {
    def reads(json: JsValue) = {
      json match {
        case jsString: JsString => {
          if (ObjectId.isValid(jsString.value)) JsSuccess(new ObjectId(jsString.value))
          else JsError("Invalid ObjectId")
        }
        case other => JsError("Can't parse json path as an ObjectId. Json content = " + other.toString())
      }
    }
    def writes(oId: ObjectId): JsValue = {
      JsString(oId.toString)
    }
  }

  implicit val objectMapWrites = Writes[Map[ObjectId, Object]] { map =>
    Json.obj(map.map {
      case (oId, o) =>
        val ret: (String, JsValueWrapper) = o match {
          case _: String => oId.toString -> JsString(o.asInstanceOf[String])
          case _         => oId.toString -> JsArray(o.asInstanceOf[List[String]].map(JsString(_)))
        }
        ret
    }.toSeq: _*)
  }

  def errorMaker(path: String, message: String) = Seq((JsPath \ path, Seq(ValidationError(message))))
  def errorMaker(errorMap: Map[String, String]) = errorMap.toSeq.map(e => (JsPath \ e._1, Seq(ValidationError(e._2))))

  // Define serialization for JSON validation error messages.
  implicit val JsPathWrites = Writes[JsPath](p => JsString(p.toString))

  implicit val ValidationErrorWrites =
    Writes[ValidationError](e => JsString(e.message))

  implicit val jsonValidateErrorWrites = (
    (JsPath \ "path").write[JsPath] and
      (JsPath \ "errors").write[Seq[ValidationError]] tupled)
}

object BSONObjectIdFormat extends Format[ObjectId] {
  def reads(json: JsValue) = {
    json match {
      case jsString: JsString => {
        if (ObjectId.isValid(jsString.value)) JsSuccess(new ObjectId(jsString.value))
        else JsError("Invalid ObjectId")
      }
      case other => JsError("Can't parse json path as an ObjectId. Json content = " + other.toString())
    }
  }
  def writes(oId: ObjectId): JsValue = {
    JsString(oId.toString)
  }
}
