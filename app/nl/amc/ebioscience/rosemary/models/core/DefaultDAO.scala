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

import com.novus.salat.Context
import nl.amc.ebioscience.rosemary.services.MongoContext
import se.radley.plugin.salat.PlaySalat
import org.bson.types.ObjectId

/**
 * WIP: To replace DefaultModelBase
 */
class DefaultDAO[T <: BaseEntity](name: String, mctx: MongoContext, pls: PlaySalat)(
    implicit mot: Manifest[T],
    mid: Manifest[DefaultDAO.Id],
    ctx: Context = mctx.salatContext,
    ps: PlaySalat = pls) extends ModelBase[T, DefaultDAO.Id](name) {

  class DefaultChildDAO[T <: BaseEntity](name: String)(
    implicit mot: Manifest[T], mid: Manifest[DefaultDAO.Id])
      extends ModelChild[T, DefaultDAO.Id](name)

  type Id = DefaultDAO.Id
}

object DefaultDAO {
  type Id = ObjectId
}
        