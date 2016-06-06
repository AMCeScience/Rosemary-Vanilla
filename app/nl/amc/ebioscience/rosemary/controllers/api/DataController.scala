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
class DataController @Inject() (securityService: SecurityService) extends Controller with JsonHelpers {

  def queryId(workspaceId: Tag.Id, id: Datum.Id) = securityService.HasToken(parse.empty) { implicit request =>
    findTag(workspaceId).map { workspaceTag =>
      Datum.findOneById(id, workspaceTag).map { datum =>
        Ok(datum.toJson)
      } getOrElse Conflict(s"Could not find datum_id $id")
    } getOrElse Conflict(s"Could not find the workspace tag $workspaceId")
  }

  def queryIds(workspaceId: Tag.Id) = securityService.HasToken(parse.json) { implicit request =>
    Logger.trace(s"Request: ${request.body}")
    (request.body \ "ids").asOpt[Set[Datum.Id]].map { ids =>
      findTag(workspaceId).map { workspaceTag =>
        Ok(Datum.findByIds(ids, workspaceTag).toJson)
      } getOrElse Conflict(s"Could not find the workspace tag $workspaceId")
    } getOrElse BadRequest(Json.toJson(errorMaker("ids", "error.path.missing")))
  }

  def children(workspaceId: Tag.Id, id: Datum.Id) = securityService.HasToken(parse.empty) { implicit request =>
    findTag(workspaceId).map { workspaceTag =>
      Datum.findOneById(id, workspaceTag).map { datum =>
        Ok(Datum.getChildren(datum, workspaceTag).toJson)
      } getOrElse Conflict(s"Could not find datum_id $id")
    } getOrElse Conflict(s"Could not find the workspace tag $workspaceId")
  }

  def parents(workspaceId: Tag.Id, id: Datum.Id) = securityService.HasToken(parse.empty) { implicit request =>
    findTag(workspaceId).map { workspaceTag =>
      Datum.findOneById(id, workspaceTag).map { datum =>
        Ok(Datum.getAssendants(datum, workspaceTag).reverse.toJson)
      } getOrElse Conflict(s"Could not find datum_id $id")
    } getOrElse Conflict(s"Could not find the workspace tag $workspaceId")
  }

  def parent(workspaceId: Tag.Id, id: Datum.Id) = securityService.HasToken(parse.empty) { implicit request =>
    findTag(workspaceId).map { workspaceTag =>
      Datum.findOneById(id, workspaceTag).map { datum =>
        Ok(Datum.getAllDirectParents(datum, workspaceTag).toJson)
      } getOrElse Conflict(s"Could not find datum_id $id")
    } getOrElse Conflict(s"Could not find the workspace tag $workspaceId")
  }

  def replicas(workspaceId: Tag.Id, id: Datum.Id) = securityService.HasToken(parse.empty) { implicit request =>
    findTag(workspaceId).map { workspaceTag =>
      Datum.findOneById(id).map { datum =>
        Ok(datum.getReplicas.toJson)
      } getOrElse Conflict(s"Could not find datum_id $id")
    } getOrElse Conflict(s"Could not find the workspace tag $workspaceId")
  }

  /** body of json requests to tag or untag some data */
  case class TagRequest(tagid: Tag.Id, dataids: List[Datum.Id])
  object TagRequest {
    implicit val tagRequestFmt = Json.format[TagRequest]
  }

  def tagOrUntag(workspaceId: Tag.Id, func: (Datum, Tag.Id) => Datum) = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")

    json.validate[TagRequest].fold(
      valid = { tagRequest =>
        Tag.findOneById(tagRequest.tagid) match {
          case Some(tag) =>
            val result = for {
              dataid <- tagRequest.dataids
              data <- Datum.findOneById(dataid)
            } yield func(data, tag.id)
            Ok(result.toJson)
          case None => BadRequest(Json.toJson(errorMaker("tagid", "not found")))
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  /** Add a tag to a list of data */
  def tag(workspaceId: Tag.Id) = tagOrUntag(workspaceId: Tag.Id, (data: Datum, tagid: Tag.Id) => data.addTag(tagid))
  /** Remove a tag from a list of data */
  def untag(workspaceId: Tag.Id) = tagOrUntag(workspaceId: Tag.Id, (data: Datum, tagid: Tag.Id) => data.removeTag(tagid))

  private def findTag(workspaceId: Tag.Id) = Tag.findOneById(workspaceId).map { tag =>
    tag match {
      case tag: WorkspaceTag => Some(tag)
      case _                 => None
    }
  }.flatten
}
