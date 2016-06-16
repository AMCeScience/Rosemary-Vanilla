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
package nl.amc.ebioscience.rosemary.models.core

import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.dao.{ SalatDAO, SalatMongoCursor, ModelCompanion }
import play.api.Logger
import se.radley.plugin.salat.PlaySalat
import nl.amc.ebioscience.rosemary.core.JJson

class ModelBase[T <: BaseEntity, I <: Any](val name: String)(
  implicit mot: Manifest[T], mid: Manifest[I], ctx: Context, ps: PlaySalat)
    extends SalatDAO[T, I](collection = ps.collection(name)) { // TODO: maybe use Salat CompanionModel?

  class ModelChild[CT <: BaseEntity, CI <: Any](val name: String, val parent: String = "parentId")(
    implicit mct: Manifest[CT], mcid: Manifest[CI])
      extends ChildCollection[CT, CI](collection = ps.collection(name), parentIdField = parent)

  def findAll() = find(MongoDBObject())
  def findByIds(ids: Set[I]): Set[T] = find("_id" $in ids).toSet
  def findByIds(ids: List[I]): List[T] = find("_id" $in ids).toList

  def removeById(id: I): WriteResult = removeById(id, defaultWriteConcern)
  def removeByIds(ids: List[I]): WriteResult = removeByIds(ids, defaultWriteConcern)

  /** Find documents in the collection that their type ends with a given string */
  def findByType(myType: String): List[T] = find("_t" $regex s".*$myType$$").toList

  def emptyCursor = find("dummy" $eq "dummy")

  implicit class Queries(entity: T) {

    // TODO: How to pass up the WriteResult?
    def save: T = {
      val wr = ModelBase.this.save(entity)
      Logger.trace("Entity saved: " + entity)
      entity
    }

    def insert: T = {
      val oi = ModelBase.this.insert(entity)
      Logger.trace("Entity inserted: " + entity)
      entity
    }

    def remove: T = {
      val wr = ModelBase.this.remove(entity)
      Logger.trace("Entity removed: " + entity)
      entity
    }

    def update: T = {
      val wr = ModelBase.this.update("_id" $eq entity.id, entity, false, false, defaultWriteConcern)
      Logger.trace("Entity updated: " + entity)
      entity
    }

  }
}

/**
 * ModelBase companion object includes implicit converters
 */
object ModelBase {

  implicit class TraversableToJson[T <: BaseEntity](l: Traversable[T])(implicit ctx: Context, m: Manifest[T]) {
    def toJsonString = grater[T].toCompactJSONArray(l)
    def toJson = JJson.toValue(grater[T].toJSONArray(l))
  }

  implicit class CursorToJson[T <: BaseEntity](l: SalatMongoCursor[T])(implicit ctx: Context, m: Manifest[T]) {
    def toJsonString = grater[T].toCompactJSONArray(l.toTraversable)
  }

  implicit class EntityToJson[T <: BaseEntity](e: T)(implicit ctx: Context, m: Manifest[T]) {
    def toJsonString = grater[T].toCompactJSON(e)
    def toJson = JJson.toValue(grater[T].toJSON(e))
  }
}
