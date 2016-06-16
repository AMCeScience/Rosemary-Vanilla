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
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.controllers.JsonHelpers
import nl.amc.ebioscience.rosemary.services.{ SecurityService, CryptoService }

@Singleton
class UsersController @Inject() (
    securityService: SecurityService,
    cryptoService: CryptoService) extends Controller with JsonHelpers {

  def index = securityService.HasToken(parse.empty) { implicit request =>
    Ok(User.findAll.toList.map(_.copy(password = "", credentials = Nil)).toJson)
  }

  case class RegistrationRequest(email: String, password: String, name: String)
  object RegistrationRequest {
    implicit val registrationRequestReads: Reads[RegistrationRequest] = (
      (JsPath \ "email").read[String](email) and
      (JsPath \ "password").read[String](minLength[String](6)) and
      (JsPath \ "name").read[String](minLength[String](5)))(RegistrationRequest.apply _)
  }

  def create = Action(parse.json) { implicit request =>
    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[RegistrationRequest].fold(
      valid = { registrationRequest =>
        User.find(registrationRequest.email) match {
          case Some(user) => BadRequest(Json.toJson(errorMaker("email", "duplicate")))
          case None =>
            val user = User(
              email = registrationRequest.email,
              password = registrationRequest.password,
              name = registrationRequest.name).hashPassword.insert
            val name = registrationRequest.name.split(' ')(0)
            val namePossessive = if (name.endsWith("s")) s"$name'" else s"$name's"
            WorkspaceTag(s"$namePossessive Workspace", Membered(user.id)).save
            Ok(user.toJson)
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  def queryIds = securityService.HasToken(parse.json) { implicit request =>
    Logger.trace("Request: " + request.body)
    (request.body \ "userids").asOpt[Set[User.Id]].map { ids =>
      Ok(User.findByIds(ids).toJson)
    } getOrElse BadRequest(Json.toJson(errorMaker("userids", "error.path.missing")))
  }

  def queryId(id: User.Id) = securityService.HasToken(parse.empty) { implicit request =>
    User.findOneById(id).map { user =>
      Ok(user.toJson)
    } getOrElse Conflict(s"Could not find user_id $id")
  }

  case class CredentialRequest(resource: Resource.Id, username: String, password: String)
  object CredentialRequest {
    implicit val credentialRequestFmt = Json.format[CredentialRequest]
  }

  def addCredential(id: User.Id) = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[CredentialRequest].fold(
      valid = { credRequest =>
        Resource.findOneById(credRequest.resource) map { resource =>
          val cred = Credential(resource.id, credRequest.username, cryptoService.encrypt(credRequest.password))
          // TODO use passed id
          Ok(User.current.addCredential(cred).toJson)
        } getOrElse Conflict(s"Could not find resource_id ${credRequest.resource}")
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  case class RoleRequest(role: String)
  object RoleRequest {
    implicit val roleRequestFmt = Json.format[RoleRequest]
  }

  def changeRole(id: User.Id) = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Role change request on user $id: $json")
    json.validate[RoleRequest].fold(
      valid = { roleRequest =>
        if (User.current.role == Role.Admin) {
          User.findOneById(id) map { user =>
            roleRequest.role match {
              case "active"   => Ok("Active status: " + user.toggleActive.active)
              case "approved" => Ok("Approved status: " + user.toggleApproved.approved)
              case _          => Conflict(s"No existing role was selected")
            }
          } getOrElse Conflict(s"Could not find user_id $id")
        } else Conflict(s"Only those with Admin role can change roles.")
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }
}
