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
package nl.amc.ebioscience.rosemary

import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import nl.amc.ebioscience.rosemary.services.MongoContext
import se.radley.plugin.salat.PlaySalat

/**
 * WIP not used currently
 */
class RosemaryApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    new RosemaryComponents(context).application
  }
}

class RosemaryComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  lazy val router = Router.empty
  
  lazy val mongoContext = injector.instanceOf(classOf[MongoContext])
  lazy val playSalat = injector.instanceOf(classOf[PlaySalat])
}
