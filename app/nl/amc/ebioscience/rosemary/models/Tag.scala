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
package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import com.mongodb.casbah.Imports._
import com.novus.salat.annotations._
import nl.amc.ebioscience.rosemary.core.WebSockets
import scala.language.postfixOps

@Salat
sealed trait Tag extends BaseEntity {
  val name: String
  val rights: Rights
  def addMember(id: User.Id): BaseEntity with Tag
  def removeMember(id: User.Id): BaseEntity with Tag
  def allUsersThatHaveAccess: Set[User.Id]
}

case class UserTag(
    name: String,
    rights: Membered = new Membered(User.current.id),
    id: Tag.Id = new Tag.Id,
    info: Info = new Info) extends Tag {

  def addMember(id: User.Id) = copy(rights = rights.addMember(id)).update
  def removeMember(id: User.Id) = copy(rights = rights.removeMember(id)).update
  def allUsersThatHaveAccess = rights.members + rights.owner
}

case class WorkspaceTag(
    name: String,
    rights: Membered,
    visible: Set[String] = Set.empty,
    id: Tag.Id = new Tag.Id,
    info: Info = new Info) extends Tag {

  def addMember(userid: User.Id) = {
    sendNotification("added", userid)
    copy(rights = rights.addMember(userid)).update
  }
  def removeMember(userid: User.Id) = {
    sendNotification("removed", userid)
    copy(rights = rights.removeMember(userid)).update
  }
  def allUsersThatHaveAccess = rights.members + rights.owner

  private def sendNotification(action: String, userid: User.Id) = {
    val notification = UserWorkspaceNotification(
      actor = User.current.id,
      action = action,
      affected = userid,
      workspace = id,
      tags = Set(id)).insert
    WebSockets.getSocket.map(_.send("notification", notification.toJson))
  }
}

case class DatumCategoryTag(
    name: String,
    rights: Everyone = new Everyone(),
    id: Tag.Id = new Tag.Id,
    info: Info = new Info) extends Tag {

  def addMember(id: User.Id) = this
  def removeMember(id: User.Id) = this
  def allUsersThatHaveAccess = User.findAll.map(_.id).toSet
}

case class ProcessingCategoryTag(
    name: String,
    rights: Everyone = new Everyone(),
    id: Tag.Id = new Tag.Id,
    info: Info = new Info) extends Tag {

  def addMember(id: User.Id) = this
  def removeMember(id: User.Id) = this
  def allUsersThatHaveAccess = User.findAll.map(_.id).toSet
}

case class ProcessingStatusTag(
    name: String,
    rights: Everyone = new Everyone(),
    id: Tag.Id = new Tag.Id,
    info: Info = new Info) extends Tag {

  def addMember(id: User.Id) = this
  def removeMember(id: User.Id) = this
  def allUsersThatHaveAccess = User.findAll.map(_.id).toSet
}

case class MessageTag(
    name: String, // subject
    body: String,
    rights: Personal, // to capture the author
    id: Tag.Id = new Tag.Id,
    info: Info = new Info) extends Tag {
  lazy val subject = name
  lazy val author = rights.owner

  def addMember(id: User.Id) = this
  def removeMember(id: User.Id) = this
  def allUsersThatHaveAccess = Set(rights.owner)
}

/**
 * SystemTag is to group some entities, for example:
 * data that has been imported : put importer userid in info
 * data that has been used in a processing : put processing id in info
 */
case class SystemTag(
    name: String,
    kind: String, // import, processing-input, processing-output
    rights: Nobody = new Nobody(),
    id: Tag.Id = new Tag.Id,
    info: Info = new Info) extends Tag {

  def addMember(id: User.Id) = this
  def removeMember(id: User.Id) = this
  def allUsersThatHaveAccess = Set.empty
}

object Tag extends DefaultModelBase[Tag]("tags") {

  collection.createIndex(("name" -> "text", "_id" -> 1, "_t" -> 1), ("default_language" -> "none"))

  def findSuperWorkspaceTag() = {
    find("visible" $eq Set("all")).toList match {
      case item :: Nil => Some(item)
      case _           => None
    }
  }

  /** Adds a member to a tag. It validates both tag and user IDs */
  def addMember(tid: Tag.Id, uid: User.Id) = for {
    tag <- Tag.findOneById(tid)
    user <- User.findOneById(uid)
  } yield tag.addMember(user.id)

  /** Removes a member from a tag. It validates both tag and user IDs */
  def removeMember(tid: Tag.Id, uid: User.Id) = for {
    tag <- Tag.findOneById(tid)
    user <- User.findOneById(uid)
  } yield tag.removeMember(user.id)

  /** Any tag that the user is its owner */
  def findForUserAsOwner(id: User.Id) = find("rights.owner" $eq id).toSet

  /** Any tag that the user is its member (and not owner) */
  def findForUserAsMember(id: User.Id) = find("rights.members" $eq id).toSet

  /** Any tag that the user has access to */
  def findForUserHasAccess(id: User.Id) =
    find($or("rights.owner" $eq id, "rights.members" $eq id)).toSet

  /** Workspace tags that the user has access to */
  def findWorkspaceTagsUserHasAccess(id: User.Id) =
    findByType("WorkspaceTag").filter(_.rights.hasAccess(id)).toSet

  /** User tags that the user has access to */
  def findUserTagsUserHasAccess(id: User.Id) =
    findByType("UserTag").filter(_.rights.hasAccess(id)).toSet

  def filterWorkspaceTags(tags: Set[Tag.Id]) =
    findByType("WorkspaceTag").map(_.id).toSet intersect tags

  def removeTag(id: Tag.Id) = {
    Datum.purgeTag(id)
    Thread.purgeTag(id)
    ProcessingGroup.purgeTag(id)
    Processing.purgeTag(id)
    Notification.purgeTag(id)
    Recipe.purgeTag(id)
    Tag.removeById(id)
  }

  /** List of datum category tags (DB objects with proper id) */
  def datumCategoriesList = findByType("DatumCategoryTag")
  def datumCategoriesNameMap = datumCategoriesList map { c => (c.name, c) } toMap
  def datumCategoriesIdMap = datumCategoriesList map { c => (c.id, c) } toMap
  def getDatumCategory(categoryName: String) = datumCategoriesNameMap.get(categoryName).getOrElse(throw new IllegalArgumentException)
  def getDatumCategory(categoryId: Tag.Id) = datumCategoriesIdMap.get(categoryId).getOrElse(throw new IllegalArgumentException)
  def getDatumCategoryOption(categoryId: Tag.Id) = datumCategoriesIdMap.get(categoryId)

  /** List of processing category tags (DB objects with proper id) */
  def processingCategoriesList = findByType("ProcessingCategoryTag")
  def processingCategoriesNameMap = processingCategoriesList map { c => (c.name, c) } toMap
  def processingCategoriesIdMap = processingCategoriesList map { c => (c.id, c) } toMap
  def getProcessingCategory(categoryName: String) = processingCategoriesNameMap.get(categoryName).getOrElse(throw new IllegalArgumentException)
  def getProcessingCategory(categoryId: Tag.Id) = processingCategoriesIdMap.get(categoryId).getOrElse(throw new IllegalArgumentException)

  /** List of processing status tags (DB objects with proper id) */
  def processingStatusTagsList = findByType("ProcessingStatusTag")
  def processingStatusTagsNameMap = processingStatusTagsList map { t => (t.name, t) } toMap
  def processingStatusTagsIdMap = processingStatusTagsList map { t => (t.id, t) } toMap
  def getProcessingStatusTag(statusTagName: String) = processingStatusTagsNameMap.get(statusTagName).getOrElse(throw new IllegalArgumentException)
  def getProcessingStatusTag(statusTagId: Tag.Id) = processingStatusTagsIdMap.get(statusTagId).getOrElse(throw new IllegalArgumentException)

  /** Names used to initialize data category tags and find them in the datumCategoriesMap */
  object DatumCategories extends Enumeration {
    // val Study, Subject, Sample, Extract, Sequence = Value
    val Project, Subject, Experiment, Scan, Reconstruction, Resource, File = Value
  }

  /** Names used to initialize data category tags and find them in the processingCategoriesMap */
  object ProcessingCategories extends Enumeration {
    // val Protocol, Assay, DataProcessing = Value
    val DataProcessing = Value
  }
}
