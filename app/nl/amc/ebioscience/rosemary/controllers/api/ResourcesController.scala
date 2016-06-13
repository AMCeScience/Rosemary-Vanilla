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
import nl.amc.ebioscience.rosemary.services.{ SecurityService, CryptoService }

@Singleton
class ResourcesController @Inject() (
    securityService: SecurityService,
    cryptoService: CryptoService) extends Controller with JsonHelpers {

  def index = securityService.HasToken(parse.empty) { implicit request =>
    Ok(Resource.findAll.toList.map(_.copy(username = None, password = None)).toJson)
  }

  case class CreateResourceRequest(
      name: String,
      kind: String,
      protocol: String,
      host: String,
      port: Option[Int],
      basePath: Option[String],
      username: Option[String], // Community username
      password: Option[String]) {

    def validateKind = kind.toLowerCase match {
      case "webdav" => Right(ResourceKind.Webdav)
      case k @ _    => Left(s"Unsupported kind $k")
    }

    def validateProtocol = protocol.toLowerCase match {
      case p @ ("http" | "https") => Right(p)
      case p @ _                  => Left(s"Unsupported protocol $p")
    }
  }
  object CreateResourceRequest {
    implicit val createResourceRequestFmt = Json.format[CreateResourceRequest]
  }

  def create = Action(parse.json) { implicit request =>
    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[CreateResourceRequest].fold(
      valid = { req =>
        req.validateKind match {
          case Right(kind) => req.validateProtocol match {
            case Right(protocol) =>
              val res = Resource(
                name = req.name,
                kind = kind,
                protocol = protocol,
                host = req.host,
                port = req.port.getOrElse(80),
                basePath = req.basePath,
                username = req.username,
                password = req.password.map(cryptoService.encrypt(_))).insert
              Ok(res.toJson)
            case Left(error) => Conflict(error)
          }
          case Left(error) => Conflict(error)
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  def queryId(id: Resource.Id) = securityService.HasToken(parse.empty) { implicit request =>
    Resource.findOneById(id).map { resource =>
      Ok(resource.toJson)
    } getOrElse Conflict(s"Could not find resource_id $id")
  }
}
