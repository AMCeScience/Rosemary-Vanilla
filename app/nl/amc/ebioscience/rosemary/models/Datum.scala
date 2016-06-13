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
import play.api.Logger
import com.mongodb.casbah.Imports._
import java.nio.charset.StandardCharsets

/**
 * Datum captures metadata and files in Rosemary.
 *
 * @param name Main title/name of this datum, mainly for UI
 * @param children ID of its children to enable hierarchical relationships
 * @param resource ID of the resource where this datum is originated/imported from
 * @param idOnResource ID of this datum on the resource that it is originated from
 * @param pathOnResource Relative path of this datum on the resource that the datum is originated from,
 * which usually also includes ID and is prepended with the resource information to get a link to
 * this datum on that resource
 * @param replicas Captures the location of files for this datum
 * @param creator ID of the user who imported or created this datum
 * @param remarks Optional high-level description for this datum, mainly for UI
 * @param tags `WorkspaceTags`, `UserTags`, `DatumCategoryTags`,
 * `MessageTags`, and `SystemTags`
 * @param valid If this datum is valid (better to invalidate it rather than delete it)
 * @param id ID of this datum in Rosemary, system provided
 * @param info Captures metadata of this datum
 */
case class Datum(
    name: String,
    children: Set[Datum.Id] = Set.empty,
    resource: Option[Resource.Id] = None,
    idOnResource: Option[String] = None,
    pathOnResource: Option[String] = None,
    replicas: Set[Replica] = Set.empty,
    creator: Option[User.Id] = None,
    remarks: Option[String] = None,
    tags: Set[Tag.Id] = Set.empty,
    valid: Boolean = true,
    id: Datum.Id = new Datum.Id,
    info: Info = new Info) extends BaseEntity with WithReplica with WithTags with Searchable {

  def addTag(id: Tag.Id) = copy(tags = tags + id).update
  def removeTag(id: Tag.Id) = copy(tags = tags - id).update

  def addChild(id: Datum.Id) = copy(children = children + id).update
  def removeChild(id: Datum.Id) = copy(children = children - id).update

  def addToDict(k: String, v: Valunit) = copy(info = info.addToDict(k, v)).update
  def addToDict(d: Map[String, Valunit]) = copy(info = info.addToDict(d)).update

  // Disabled
  def hideInfoFields(tag: WorkspaceTag) = copy(info = info) //info = info.copy(dict = info.dict filterKeys tag.visible))

  def checkAccess(tag: WorkspaceTag) = tags.contains(tag.id)

  // Helper methods to export metadata 
  def getKeyValues: List[(String, String)] =
    List(("Name", name),
      ("Category", getCategoryName.getOrElse("Unknown")),
      ("Created", info.created.toString),
      ("Remarks", remarks.getOrElse(""))) :::
      info.dict.map(e => (e._1, e._2.value)).toList
  def getCSV: String = getKeyValues.map(e => "\"" + e._1 + "\", \"" + e._2 + "\"") mkString "\n"
  def getCSVBytes: Array[Byte] = getCSV.getBytes(StandardCharsets.UTF_8)
}

/**
 * Datum companion object that contains database queries specific to the `data` collection.
 */
object Datum extends DefaultModelBase[Datum]("data") with TagsQueries[Datum] {

  collection.createIndex("tags" $eq 1)

  def findAll(workspaceTag: WorkspaceTag) =
    find("tags" $eq workspaceTag.id).toList.map { datum => datum.hideInfoFields(workspaceTag) }

  def findOneById(id: Datum.Id, workspaceTag: WorkspaceTag) = {
    find($and(("_id" -> id), ("tags" -> workspaceTag.id))).toList match {
      case Nil =>
        Logger.error(s"Could not find a datum with id = $id, illegal access?"); None
      case item :: Nil => Some(item.hideInfoFields(workspaceTag))
      case _           => Logger.error(s"Multiple data are found with id = $id"); None
    }
  }

  def findByIds(ids: Set[Datum.Id], workspaceTag: WorkspaceTag) =
    find($and(("_id" $in ids), ("tags" $eq workspaceTag.id))).toList.map { datum => datum.hideInfoFields(workspaceTag) }

  /** Get a single direct parent filtered, the parent is chosen randomly in case there are more than one parents */
  def getSingleParent(child: Datum, workspaceTag: WorkspaceTag) = getAllDirectParents(child, workspaceTag).headOption

  /** Get all direct parents filtered */
  def getAllDirectParents(child: Datum, workspaceTag: WorkspaceTag) =
    find($and(("children" $eq child.id), ("tags" $eq workspaceTag.id))).toList

  /** Get a single direct parent unfiltered, the parent is chosen randomly in case there are more than one parents */
  def getSingleParent(child: Datum) = getAllDirectParents(child).headOption
  def getSingleParent(childid: Datum.Id) = getAllDirectParents(childid).headOption

  /** Get all direct parents unfiltered */
  def getAllDirectParents(child: Datum) =
    find("children" $eq child.id).toList
  def getAllDirectParents(childid: Datum.Id) =
    find("children" $eq childid).toList

  /** Get children filtered by the workspace */
  def getChildren(parent: Datum, workspaceTag: WorkspaceTag) =
    find($and(("_id" $in parent.children), ("tags" $eq workspaceTag.id))).toList //.map { datum => datum.hideInfoFields(workspaceTag) }

  /** Get children unfiltered */
  def getChildren(parent: Datum) =
    find("_id" $in parent.children).toList

  /** Go all the way up in the family tree using filtered single parents */
  def getAssendants(child: Datum, workspaceTag: WorkspaceTag): List[Datum] =
    getSingleParent(child, workspaceTag) match {
      case None         => Nil
      case Some(parent) => parent :: getAssendants(parent, workspaceTag)
    }
}
