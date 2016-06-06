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
import nl.amc.ebioscience.rosemary.controllers.{ JsonHelpers, EnumJson }
import nl.amc.ebioscience.rosemary.core.search.{ SearchReader, SearchWriter, SupportedTypes }
import nl.amc.ebioscience.rosemary.core.HelperTools
import nl.amc.ebioscience.rosemary.services.SecurityService

@Singleton
class SearchController @Inject() (securityService: SecurityService) extends Controller with JsonHelpers {

  /** body of JSON requests to query data or processing */
  case class QueryRequest(
    tags: Option[Set[Tag.Id]],
    page: Option[Int],
    kind: Option[SupportedTypes.Value],
    query: Option[String])
  object QueryRequest {
    implicit val supportedTypesFmt = EnumJson.enumFormat(SupportedTypes)
    implicit val queryRequestFmt = Json.format[QueryRequest]
  }

  def query = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    json.validate[QueryRequest].fold(
      valid = { queryRequest =>

        // if tags is empty autofill it with all workspace tags that the user has access to
        val tags = queryRequest.tags.getOrElse(Set.empty).toList

        // if page is not provided or invalid we assume it's 0
        val page = queryRequest.page match {
          case Some(number) if number >= 0 => number
          case _                           => 0
        }

        // Determine what Tag.Id belongs to a WorkspaceTag and what to any other Tag
        val (workspaceTags, otherTags) = Tag.findByIds(tags).partition(_.isInstanceOf[WorkspaceTag])

        val workspaceTagIds = workspaceTags match {
          case Nil => User.current.getWorkspaceTagsHasAccess.toList.map(_.id)
          case _   => workspaceTags.map(_.id)
        }
        Logger.trace(s"Workspace Tag IDs: ${workspaceTagIds.toString}")

        val otherTagIds = otherTags.map(_.id)
        Logger.trace(s"Other Tag IDs: ${otherTagIds.toString}")

        val result = queryRequest.query match {

          case Some(query) if !query.isEmpty =>
            SearchReader.search(
              query,
              workspaceTags = workspaceTagIds,
              tags = otherTagIds,
              kind = queryRequest.kind,
              page) match {
                case Right(ids) =>
                  Datum.findByIds(ids) ::: Processing.findByIds(ids) ::: ProcessingGroup.findByIds(ids)
                case Left(error) => {
                  Logger.debug(error)
                  List.empty
                }
              }

          case _ => queryRequest.kind match {
            case Some(requestedType) => requestedType match {
              case SupportedTypes.Datum =>
                Datum.findWithAnyWorkspaceTagAndWithAllTags(workspaceTagIds, otherTagIds, page)
              case SupportedTypes.Processing =>
                Processing.findWithAnyWorkspaceTagAndWithAllTags(workspaceTagIds, otherTagIds, page)
              case SupportedTypes.ProcessingGroup =>
                ProcessingGroup.findWithAnyWorkspaceTagAndWithAllTags(workspaceTagIds, otherTagIds, page)
            }
            case None => {
              val dc = Datum.findWithAnyWorkspaceTagAndWithAllTagsNoPage(workspaceTagIds, otherTagIds)
              val pc = Processing.findWithAnyWorkspaceTagAndWithAllTagsNoPage(workspaceTagIds, otherTagIds)
              val pgc = ProcessingGroup.findWithAnyWorkspaceTagAndWithAllTagsNoPage(workspaceTagIds, otherTagIds)
              val pageSize = Datum.pageSize
              val mc = dc ++ pc ++ pgc
              mc.drop(page * pageSize).take(pageSize).toList
            }
          }
        }

        Logger.trace(s"Result: ${result.toJson}")
        Ok(result.toJson)
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }
}
