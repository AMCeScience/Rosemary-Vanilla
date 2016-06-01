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
import nl.amc.ebioscience.rosemary.services.Security
import org.bson.types.ObjectId

@Singleton
class RecipesController @Inject() (security: Security) extends Controller with JsonHelpers {

  def indexApplications = security.HasToken(parse.empty) { implicit request =>
    Ok(Recipe.getApplications.toJson)
  }

  def createPipeline = security.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    NotImplemented
  }

  def query = security.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    NotImplemented
  }

  def queryIds = security.HasToken(parse.json) { implicit request =>
    Logger.trace(s"Request: ${request.body}")
    (request.body \ "ids").asOpt[Set[Recipe.Id]].map { ids =>
      Ok(Recipe.findByIds(ids).toJson)
    } getOrElse BadRequest(Json.toJson(errorMaker("ids", "error.path.missing")))
  }

  def queryId(id: Recipe.Id) = security.HasToken(parse.empty) { implicit request =>
    Recipe.findOneById(id).map { recipe =>
      Ok(recipe.toJson)
    } getOrElse Conflict(s"Could not find recipe_id $id")
  }

  def edit(id: Recipe.Id) = security.HasToken(parse.json) { implicit request =>
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

  def replicas(id: Recipe.Id) = security.HasToken(parse.empty) { implicit request =>
    Recipe.findOneById(id) map { recipe =>
      Ok(recipe.getReplicas.toJson)
    } getOrElse Conflict(s"Could not find recipe_id $id")
  }
}
