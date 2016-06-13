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
import com.mongodb.casbah.Imports._
import com.novus.salat.annotations._
import play.api.Logger

@Salat
trait WithTags extends BaseEntity {
  val tags: Set[Tag.Id]

  def getCategoryTags = {
    Tag.findByIds(tags).filter { t => t.isInstanceOf[DatumCategoryTag] || t.isInstanceOf[ProcessingCategoryTag] }
  }

  def getProcessingStatusTags = {
    Tag.findByIds(tags).filter { t => t.isInstanceOf[ProcessingStatusTag] }
  }

  def getWorkspaceTags = {
    Tag.findByIds(tags).filter { t => t.isInstanceOf[WorkspaceTag] }
  }

  def getCategoryName = {
    val allCatTags = Tag.datumCategoriesIdMap ++ Tag.processingCategoriesIdMap
    val thisCatTags = tags intersect allCatTags.keys.toSet
    thisCatTags.size match {
      case 0 =>
        Logger.error(s"$id has no category"); None
      case 1 => Some(allCatTags.get(thisCatTags.head).get.name)
      case _ => Logger.error(s"$id has multiple categories"); None
    }
  }
}

trait TagsQueries[T <: WithTags] {
  self: DefaultModelBase[T] =>

  val pageSize = 10

  def findByIdsAndAllTags(ids: Set[Id], tags: Set[Tag.Id]) =
    find($and(("_id" $in ids), ("tags" $all tags))).toList

  def findByIdsAndAnyTags(ids: Set[Id], tags: Set[Tag.Id]) =
    find($and(("_id" $in ids), ("tags" $in tags))).toList

  def findWithAllTags(tags: Set[Tag.Id], page: Int) =
    findWithAllTagsNoPage(tags).skip(page * pageSize).limit(pageSize).toList

  def findWithAllTagsNoPage(tags: Set[Tag.Id]) =
    find("tags" $all tags).sort("info.created" $eq -1)

  /** find entities that have any of the given tags, sorted in reverse chronological order, paged in pages from 0+ */
  def findWithAnyTags(tags: Set[Tag.Id], page: Int) =
    findWithAnyTagsNoPage(tags).skip(page * pageSize).limit(pageSize).toList

  /** find entities that have any of the given tags sorted reverse by the time created */
  def findWithAnyTagsNoPage(tags: Set[Tag.Id]) =
    find("tags" $in tags).sort("info.created" $eq -1)

  def findWithAnyWorkspaceTagAndWithAllTagsNoPage(wsTags: List[Tag.Id], tags: List[Tag.Id]) =
    (wsTags, tags) match {
      case (Nil, Nil) => emptyCursor // TODO this should give an empty SalatMongoCursor
      case (wts, Nil) => findWithAnyTagsNoPage(wts.toSet)
      case (Nil, ots) => findWithAllTagsNoPage(ots.toSet)
      case (wts, ots) => find($and(("tags" $in wsTags), ("tags" $all tags))).sort("name" $eq 1)
    }

  def findWithAnyWorkspaceTagAndWithAllTags(wsTags: List[Tag.Id], tags: List[Tag.Id], page: Int) =
    findWithAnyWorkspaceTagAndWithAllTagsNoPage(wsTags, tags).skip(page * pageSize).limit(pageSize).toList

  /** tag an entity */
  def tagEntity(entityId: Id, tagId: Tag.Id) = update(
    MongoDBObject("_id" -> entityId),
    $addToSet("tags" -> tagId))

  /** tag all entities given that they have 'tags' filed*/
  def tagEntities(entityIds: Set[Id], tagId: Tag.Id) = update(
    "_id" $in entityIds,
    $addToSet("tags" -> tagId),
    false, true)

  /** removes tag id from all the items in the collection */
  def purgeTag(tagId: Id) = update(
    MongoDBObject("tags" -> tagId),
    $pull("tags" -> tagId),
    false, true)
}
