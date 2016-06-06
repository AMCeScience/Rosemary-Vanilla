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
import nl.amc.ebioscience.rosemary.services.SecurityService

@Singleton
class NotificationsController @Inject() (securityService: SecurityService) extends Controller with JsonHelpers {

  case class NotificationListRequest(workspace: Tag.Id, page: Option[Int])
  object NotificationListRequest {
    implicit val notificationListRequestFmt = Json.format[NotificationListRequest]
  }

  def query = securityService.HasToken(parse.json) { implicit request =>
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
