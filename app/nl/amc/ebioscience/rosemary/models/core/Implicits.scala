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

import play.api.Application
import nl.amc.ebioscience.rosemary.services.MongoContext
import se.radley.plugin.salat.PlaySalat
import com.novus.salat.Context

object Implicits {

  implicit val app: play.api.Application = play.api.Play.current

  private val mongoContextCache = Application.instanceCache[MongoContext]
  private val playSalatCache = Application.instanceCache[PlaySalat]

  implicit def mongoContext(implicit application: Application): MongoContext = mongoContextCache(application)
  implicit def salatContext(implicit application: Application): Context = mongoContext(application).salatContext
  implicit def playSalat(implicit application: Application): PlaySalat = playSalatCache(application)
}