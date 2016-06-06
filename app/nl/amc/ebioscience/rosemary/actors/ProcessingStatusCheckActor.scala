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
package nl.amc.ebioscience.rosemary.actors

import javax.inject._
import akka.actor.Actor
import play.api.Logger
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.services.processing.ProcessingHelper
import akka.actor.actorRef2Scala

class ProcessingStatusCheckActor @Inject() (processingHelper: ProcessingHelper) extends Actor {

  def receive = {
    case _ =>
      Logger.debug("Sit tight! Going to update status of all processings!")

      val processingGroups = ProcessingGroup.findAll

      // Update status and store in DB
      processingGroups.foreach(processingHelper.updateStatusAndSendNotification(_))

      Logger.debug("Status updates are done! so long!")
      sender ! "Done!"
  }
}
