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
import nl.amc.ebioscience.rosemary.core.JJson
import nl.amc.ebioscience.processingmanager.types.ProcessingLifeCycle
import nl.amc.ebioscience.rosemary.services.SecurityService

@Singleton
class TagsController @Inject() (securityService: SecurityService) extends Controller with JsonHelpers {

  def queryId(id: Tag.Id) = securityService.HasToken(parse.empty) { implicit request =>
    Tag.findOneById(id).map { tag =>
      Ok(tag.toJson)
    } getOrElse Conflict(s"Could not find tag_id $id")
  }

  def delete(id: Tag.Id) = securityService.HasToken(parse.empty) { implicit request =>
    Tag.findOneById(id).map { tag =>
      if (tag.rights.isOwner(User.current.id)) Ok(JJson.writeValueAsString(Tag.removeTag(id)))
      else Conflict("You cannot delete this workspace because you are not its owner!")
    } getOrElse Conflict(s"Could not find tag_id $id")
  }

  def createUserTag = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    (json \ "name").asOpt[String].map { tagname =>
      if (!User.current.getUserTagsHasAccess.map(_.name).contains(tagname)) {
        val tag = UserTag(tagname, Membered(User.current.id)).save
        Redirect(s"/api/v1/tags/${tag.id}")
      } else Conflict(s"Tag $tagname already exists.")
    } getOrElse BadRequest(Json.toJson(errorMaker("name", "error.path.missing")))
  }

  def createWorkspaceTag = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    (json \ "name").asOpt[String].map { tagname =>
      if (!User.current.getWorkspaceTagsHasAccess.map(_.name).contains(tagname)) {
        val tag = WorkspaceTag(tagname, Membered(User.current.id)).save
        Redirect(s"/api/v1/tags/${tag.id}")
      } else Conflict(s"Workspace $tagname already exists.")
    } getOrElse BadRequest(Json.toJson(errorMaker("name", "error.path.missing")))
  }

  case class TagMemberRequest(userids: Set[User.Id])
  object TagMemberRequest {
    implicit val tagMemberRequestFmt = Json.format[TagMemberRequest]
  }

  private def tagMemberManipulation(func: (User.Id) => Option[BaseEntity with Tag]) =
    securityService.HasToken(parse.json) { implicit request =>
      val json = request.body
      Logger.trace(s"Request: $json")
      json.validate[TagMemberRequest].fold(
        valid = { tagMemberRequest =>
          val result = for {
            userid <- tagMemberRequest.userids
          } yield func(userid)
          Ok(result.flatten.toJson)
        },
        invalid = {
          errors => BadRequest(Json.toJson(errors))
        })
    }

  def addMembers(tid: Tag.Id) = tagMemberManipulation((uid: User.Id) => Tag.addMember(tid, uid))
  def removeMembers(tid: Tag.Id) = tagMemberManipulation((uid: User.Id) => Tag.removeMember(tid, uid))

  /** body of JSON requests to tag or untag some data or processing */
  case class TagRequest(
    tagid: Tag.Id,
    dataids: Option[Set[Datum.Id]],
    processingids: Option[Set[Processing.Id]],
    processinggroupids: Option[Set[ProcessingGroup.Id]],
    recipeids: Option[Set[Recipe.Id]])
  object TagRequest {
    implicit val tagRequestFmt = Json.format[TagRequest]
  }

  /** Add a tag to a list of data or processing or recipes */
  def tagEntities = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    json.validate[TagRequest].fold(
      valid = { tagRequest =>
        Tag.findOneById(tagRequest.tagid) match {
          case Some(tag) =>
            if (tagRequest.dataids.isDefined) Datum.tagEntities(tagRequest.dataids.get, tag.id)
            if (tagRequest.processingids.isDefined) Processing.tagEntities(tagRequest.processingids.get, tag.id)
            if (tagRequest.processinggroupids.isDefined) ProcessingGroup.tagEntities(tagRequest.processinggroupids.get, tag.id)
            if (tagRequest.recipeids.isDefined) Recipe.tagEntities(tagRequest.recipeids.get, tag.id)
            Ok(Json.obj("status" -> "OK"))
          case None => BadRequest(Json.toJson(errorMaker("tagid", "not found")))
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  /** Remove a tag from a list of data or processing or recipes */
  def untagEntities = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    json.validate[TagRequest].fold(
      valid = { tagRequest =>
        NotImplemented
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  /** Any tag that the user has access to */
  def queryAccess = securityService.HasToken(parse.empty) { implicit request =>
    Ok(User.current.getTagsHasAccess.toJson)
  }

  case class SummaryRequest(tagids: Set[Tag.Id])
  object SummaryRequest {
    implicit val summaryReqFmt = Json.format[SummaryRequest]
  }

  case class CatCount(catname: String, count: Int)
  object CatCount {
    implicit val catcountFmt = Json.format[CatCount]
  }
  case class SummaryItem(tagid: Tag.Id, summary: Set[CatCount])
  object SummaryItem {
    implicit val summaryItemFmt = Json.format[SummaryItem]
  }

  def summaryWorkspaceTags = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    json.validate[SummaryRequest].fold(
      valid = { req =>
        val res = Tag.findByIds(req.tagids).filter(_.isInstanceOf[WorkspaceTag]).map { wt =>

          val ds = Datum.findWithAllTagsNoPage(Set(wt.id)).toSet
          val dscs = ds groupBy (_.getCategoryName.getOrElse("unknown")) mapValues (_.size)
          val dscsc = dscs.map { i => CatCount(i._1, i._2) }.toSet

          val ps = Processing.findWithAllTagsNoPage(Set(wt.id)).toSet
          val pscs = ps groupBy (_.getCategoryName.getOrElse("unknown")) mapValues (_.size)
          val pscsc = pscs.map { i => CatCount(s"Processing_${i._1}", i._2) }.toSet

          val pgs = ProcessingGroup.findWithAllTagsNoPage(Set(wt.id)).toSet
          val pgscs = pgs groupBy (_.getCategoryName.getOrElse("unknown")) mapValues (_.size)
          val pgscsc = pgscs.map { i => CatCount(s"ProcessingGroup_${i._1}", i._2) }.toSet

          SummaryItem(wt.id, dscsc ++ pscsc ++ pgscsc)
        }
        Ok(Json.toJson(res))
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  /** All datum category tags */
  def indexDatumCategories = Action {
    Ok(Tag.datumCategoriesList.toJson)
  }

  /** All processing category tags */
  def indexProcessingCategories = Action {
    Ok(Tag.processingCategoriesList.toJson)
  }

  /** All processing status tags */
  def indexProcessingStatusTags = Action {
    Ok(Tag.processingStatusTagsList.toJson)
  }

  /**
   * From the list below, if a category tag is not in the database it will create one for it
   * Good for adding categories that come later
   */
  def initializePublicTags = Action {
    for (cat <- Tag.DatumCategories.values if Tag.datumCategoriesNameMap.get(cat.toString).isEmpty) {
      DatumCategoryTag(cat.toString).save
    }
    for (cat <- Tag.ProcessingCategories.values if Tag.processingCategoriesNameMap.get(cat.toString).isEmpty) {
      ProcessingCategoryTag(cat.toString).save
    }
    for (st <- ProcessingLifeCycle.values if Tag.processingStatusTagsNameMap.get(st.toString).isEmpty) {
      ProcessingStatusTag(st.toString).save
    }
    Ok("Public Tags Updated!")
  }
}
