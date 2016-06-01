package nl.amc.ebioscience.rosemary.core.processing

import akka.actor.Actor
import play.api.Logger
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._

class ProcessingManagerActor extends Actor {

  def receive = {
    case _ =>
      Logger.debug("Sit tight! Going to update status of all processings!")

      val processingGroups = ProcessingGroup.findAll

      // Update status and store in DB
      processingGroups.foreach(updateStatusAndSendNotification(_))

      Logger.debug("Status updates are done! so long!")
      sender ! "Done!"
  }
}
