package nl.amc.ebioscience.rosemary.services.processing

import javax.inject._
import play.api.Logger
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.core.processing._
import nl.amc.ebioscience.rosemary.core.WebSockets
import nl.amc.ebioscience.processingmanager.types.ProcessingLifeCycle
import scala.reflect.runtime.universe

@Singleton
class ProcessingHelper @Inject() (processingManagerClient: ProcessingManagerClient) {

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

          // run-time binding using the Scala reflection API
          val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
          val module = runtimeMirror.staticModule(application.transformer)
          val transformer = runtimeMirror.reflectModule(module).instance.asInstanceOf[Transformer]

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

          // run-time binding using the Scala reflection API
          val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
          val module = runtimeMirror.staticModule(application.transformer)
          val transformer = runtimeMirror.reflectModule(module).instance.asInstanceOf[Transformer]

          // Update submission statuses and create new datum if they are produced by the Processing Manager  
          val updatedProcessing = transformer.getSpark(statusContainerMsg)

          // TODO Send notification about newly added data

          updatedProcessing
      }
    }
}
