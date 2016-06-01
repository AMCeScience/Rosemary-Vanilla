package nl.amc.ebioscience.rosemary.controllers.api

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.controllers.JsonHelpers
import nl.amc.ebioscience.rosemary.services.Security

@Singleton
class NotificationsController @Inject() (security: Security) extends Controller with JsonHelpers {

  case class NotificationListRequest(workspace: Tag.Id, page: Option[Int])
  object NotificationListRequest {
    implicit val notificationListRequestFmt = Json.format[NotificationListRequest]
  }

  def query = security.HasToken(parse.json) { implicit request =>
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
