package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import play.api.Logger
import com.mongodb.casbah.Imports._
import java.nio.charset.StandardCharsets

/** Datum is a meta-data entry in the Rosemary
  *
  * @param children To enable hierarchical relationships
  * @param resource Where the meta-data for this data is originated from
  * @param path Relative path to query
  * @param replicas Where are the files for this data are saved
  * @param tags User and system tags
  */
case class Datum(
    name: String,
    children: Set[Datum.Id] = Set.empty, // ID of its children
    resource: Option[Resource.Id] = None, // The resource from which this data is imported from
    idOnResource: Option[String] = None, // ID of this data on that resource
    pathOnResource: Option[String] = None, // Path on that resource (which usually also includes ID)
    replicas: Set[Replica] = Set.empty, // If this data is a file, it has at least one replica
    creator: Option[User.Id] = None,
    remarks: Option[String] = None,
    tags: Set[Tag.Id] = Set.empty, // WorkspaceTags, UserTags, CategoryTags, MessageTags, SystemTags
    valid: Boolean = true,
    id: Datum.Id = new Datum.Id,
    info: Info = new Info // (Key -> (Value, Unit)) map to store additional information (meta-data)
    ) extends BaseEntity with WithReplica with WithTags with Searchable {

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

object Datum extends DefaultModelBase[Datum]("data") with TagsQueries[Datum] {

  collection.ensureIndex(("tags" -> 1))

  def findAll(workspaceTag: WorkspaceTag) =
    find("tags" -> workspaceTag.id).toList.map { datum => datum.hideInfoFields(workspaceTag) }

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
