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

import org.bson.types.ObjectId
import com.novus.salat.Context
import se.radley.plugin.salat.PlaySalat
import nl.amc.ebioscience.rosemary.services.MongoContext
import play.api.Play

/**
 * Defines the default ModelBase
 * The default ModelBase has the Id type of ObjectId
 */
class DefaultModelBase[T <: BaseEntity](name: String)(
  implicit mot: Manifest[T],
  mid: Manifest[DefaultModelBase.Id],
  ctx: Context, //= Play.current.injector.instanceOf(classOf[MongoContext]).salatContext,
  ps: PlaySalat) // = Play.current.injector.instanceOf(classOf[PlaySalat]))
    extends ModelBase[T, DefaultModelBase.Id](name) {

  class DefaultModelChild[T <: BaseEntity](name: String)(
    implicit mot: Manifest[T], mid: Manifest[DefaultModelBase.Id])
      extends ModelChild[T, DefaultModelBase.Id](name)

  type Id = DefaultModelBase.Id
}

object DefaultModelBase {
  type Id = ObjectId
}
