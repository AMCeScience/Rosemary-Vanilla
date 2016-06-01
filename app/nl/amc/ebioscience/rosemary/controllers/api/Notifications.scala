package nl.amc.ebioscience.rosemary.controllers.api

import play.api._
import play.api.mvc._
import play.api.libs.json._
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.PlayContext.salatContext
import nl.amc.ebioscience.rosemary.controllers.Security
import nl.amc.ebioscience.rosemary.controllers.JsonHelpers

object Notifications extends Controller with Security with JsonHelpers {

  case class NotificationListRequest(workspace: Tag.Id, page: Option[Int])
  object NotificationListRequest {
    implicit val notificationListRequestFmt = Json.format[NotificationListRequest]
  }

  def query = HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[NotificationListRequest].fold(
      valid = { notificationListRequest =>
        val workspace = notificationListRequest.workspace
        val page = notificationListRequest.page match {
          case Some(number) if number >= 0 => number
          case _                           => 0
        }
        Ok(Notification.findWithAnyTags(Set(workspace), page).toList.toJson)
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }
}
