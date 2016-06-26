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
package nl.amc.ebioscience.rosemary.services.processing

import javax.inject._
import play.api.Logger
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.core.WebSockets
import nl.amc.ebioscience.processingmanager.types.ProcessingLifeCycle
import scala.reflect.runtime.universe
import play.api.inject.{ QualifierInstance, BindingKey }
import play.api.{ Application => PlayApplication }
import com.google.inject.name.Names

@Singleton
class ProcessingHelper @Inject() (
    processingManagerClient: ProcessingManagerClient,
    playApplication: Provider[PlayApplication]) {

  /**
   * Update status and send notification for a ProcessingGroup
   */
  def updateStatusAndSendNotification(processingGroup: ProcessingGroup) = {
    val previousStatus = processingGroup.lastStatus

    val updatedProcessingGroup = previousStatus match {
      case None => fetchAndUpdateStatus(processingGroup)
      case Some(status) => status match {
        case st @ (ProcessingLifeCycle.Done | ProcessingLifeCycle.Aborted) =>
          Logger.debug(s"Processing Group ${processingGroup.id} has terminal status: $st, nothing to do here.")
          None
        case _ => fetchAndUpdateStatus(processingGroup)
      }
    }

    // Send notification about the change in the status of the ProcessingGroup
    updatedProcessingGroup.map { upg =>
      if (upg.lastStatus != previousStatus) {

        val wTags = Tag.filterWorkspaceTags(processingGroup.tags)
        val users = Tag.findByIds(wTags).flatMap(_.allUsersThatHaveAccess)

        // Create notification and store in DB (if status is changed)
        val notification = ProcessingNotification(
          processing = processingGroup.id,
          status = upg.lastStatus.get,
          tags = wTags).insert

        // Send notification via WebSocket
        val socket = WebSockets.getSockets(users)
        socket.map(_.send("notification", notification.toJson))
      }
    }

    updatedProcessingGroup
  }

  /**
   * Update status and send notification for a Processing
   * <p>
   * <i>Note: Send notification is not implemented yet</i>
   */
  def updateStatusAndSendNotification(processing: Processing) = {
    val previousStatus = processing.lastStatus

    val updatedProcessing = previousStatus match {
      case None => fetchAndUpdateStatus(processing)
      case Some(status) => status match {
        case st @ (ProcessingLifeCycle.Done | ProcessingLifeCycle.Aborted) =>
          Logger.debug(s"Processing ${processing.id} has terminal status: $st, nothing to do here.")
          None
        case _ => fetchAndUpdateStatus(processing)
      }
    }

    //TODO Send notification about the change in the status of the Processing

    updatedProcessing
  }

  /**
   * Call the processing manager service and update status in the model for a ProcessingGroup
   */
  private def fetchAndUpdateStatus(processingGroup: ProcessingGroup) =
    processingManagerClient.statusProcessingGroup(processingGroup.id) match {
      case Left(e) => None
      case Right(optGroupStatusMsg) => optGroupStatusMsg match {
        case None => None
        case Some(groupStatusMsg) =>
          val application = Recipe.findByIds(processingGroup.recipes).filter(_.isInstanceOf[Application]).map(_.asInstanceOf[Application]).head

          // run-time binding using the dependency injection API
          val qualifier = Some(QualifierInstance(Names.named(application.transformer)))
          val bindingKey = BindingKey[Transformer](classOf[Transformer], qualifier)
          val transformer = playApplication.get.injector.instanceOf[Transformer](bindingKey)

          // Update submission statuses and create new datum if they are produced by the Processing Manager  
          val processings = groupStatusMsg.statuses.flatMap(transformer.getSpark(_))

          // TODO Send notification about newly added data

          // Update status of the ProcessingGroup if it differs from the last two statuses, otherwise 
          val numsProgress = processings.map(_.progress)
          val avgProgress = (numsProgress.reduce(_ + _) / numsProgress.size.toDouble).round.toInt

          val statusSet = processings.flatMap(_.lastStatus).groupBy(s => s).keySet
          val avgStatus = if (statusSet.size == 1) statusSet.head
          else if (statusSet.exists(_ == ProcessingLifeCycle.InPreparation)) ProcessingLifeCycle.InPreparation
          else if (statusSet.exists(_ == ProcessingLifeCycle.StageIn)) ProcessingLifeCycle.StageIn
          else if (statusSet.exists(_ == ProcessingLifeCycle.Submitting)) ProcessingLifeCycle.Submitting
          else if (statusSet.exists(_ == ProcessingLifeCycle.InProgress)) ProcessingLifeCycle.InProgress
          else if (statusSet.exists(_ == ProcessingLifeCycle.OnHold)) ProcessingLifeCycle.OnHold
          else if (statusSet.exists(_ == ProcessingLifeCycle.StageOut)) ProcessingLifeCycle.StageOut
          else if (statusSet.exists(_ == ProcessingLifeCycle.Aborted)) ProcessingLifeCycle.Aborted
          else if (statusSet.exists(_ == ProcessingLifeCycle.Unknown)) ProcessingLifeCycle.Unknown
          else ProcessingLifeCycle.Done

          val (utstatuses, uttags, utprogress) =
            processingGroup.getToUpdateStatusesTagsProgress(avgStatus, Some(avgProgress))

          val upg = processingGroup.copy(
            progress = utprogress,
            statuses = utstatuses,
            tags = uttags).update

          Some(upg)
      }
    }

  /**
   * Call the processing manager and update status in the model for a Processing
   */
  private def fetchAndUpdateStatus(processing: Processing) =
    processingManagerClient.statusProcessing(processing.id) match {
      case Left(e) => None
      case Right(optStatusContainerMsg) => optStatusContainerMsg match {
        case None => None
        case Some(statusContainerMsg) =>
          val application = Recipe.findByIds(processing.recipes).filter(_.isInstanceOf[Application]).map(_.asInstanceOf[Application]).head

          // run-time binding using the dependency injection API
          val qualifier = Some(QualifierInstance(Names.named(application.transformer)))
          val bindingKey = BindingKey[Transformer](classOf[Transformer], qualifier)
          val transformer = playApplication.get.injector.instanceOf[Transformer](bindingKey)

          // Update submission statuses and create new datum if they are produced by the Processing Manager  
          val updatedProcessing = transformer.getSpark(statusContainerMsg)

          // TODO Send notification about newly added data

          updatedProcessing
      }
    }
}
