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
import nl.amc.ebioscience.rosemary.core.HelperTools
import nl.amc.ebioscience.rosemary.core.Tools.Slugify
import nl.amc.ebioscience.rosemary.core.datasource.Webdav
import nl.amc.ebioscience.rosemary.services.SecurityService
import org.bson.types.ObjectId

@Singleton
class RecipesController @Inject() (securityService: SecurityService) extends Controller with JsonHelpers {

  def indexApplications = securityService.HasToken(parse.empty) { implicit request =>
    Ok(Recipe.getApplications.toJson)
  }

  def createPipeline = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    NotImplemented
  }

  def query = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    NotImplemented
  }

  def queryIds = securityService.HasToken(parse.json) { implicit request =>
    Logger.trace(s"Request: ${request.body}")
    (request.body \ "ids").asOpt[Set[Recipe.Id]].map { ids =>
      Ok(Recipe.findByIds(ids).toJson)
    } getOrElse BadRequest(Json.toJson(errorMaker("ids", "error.path.missing")))
  }

  def queryId(id: Recipe.Id) = securityService.HasToken(parse.empty) { implicit request =>
    Recipe.findOneById(id).map { recipe =>
      Ok(recipe.toJson)
    } getOrElse Conflict(s"Could not find recipe_id $id")
  }

  def edit(id: Recipe.Id) = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    val workspaceTags = (request.body \ "workspace").asOpt[Tag.Id] match {
      case Some(workspace) => Set(workspace)
      // if no workspace is defined we assume all workspaces
      case _               => User.current.getWorkspaceTagsHasAccess.map(_.id)
    }
    Logger.debug("Workspace tag ids: " + workspaceTags)
    Ok(Recipe.findWithAnyTagsNoPage(workspaceTags).toList.toJson)
  }

  def replicas(id: Recipe.Id) = securityService.HasToken(parse.empty) { implicit request =>
    Recipe.findOneById(id) map { recipe =>
      Ok(recipe.getReplicas.toJson)
    } getOrElse Conflict(s"Could not find recipe_id $id")
  }
}
