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
import dispatch._
import dispatch.Defaults._
import play.api.libs.json._
import play.api.data.validation.ValidationError
import play.api.Logger
import org.bson.types.ObjectId
import nl.amc.ebioscience.processingmanager.types.messaging.{ ActionMessage, ProcessingMessage, StatusContainerMessage, GroupStatusMessage, PortMessagePart }
import nl.amc.ebioscience.processingmanager.types.Credentials
import nl.amc.ebioscience.rosemary.services.ConfigService

/**
 * Client that uses the API of the ProcessingManager
 */
@Singleton
class ProcessingManagerClient @Inject() (configService: ConfigService) {

  val baseUri = configService.getStringConfig("processingmanager.uri")

  val pmUserId = configService.getIntConfig("processingmanager.default.userid")
  val pmProjectId = configService.getIntConfig("processingmanager.default.projectid")
  val baseReq = url(baseUri)

  Http.configure(_.setAllowPoolingConnection(true).
    setCompressionEnabled(true).
    setConnectionTimeoutInMs(10000).
    setRequestTimeoutInMs(150000))

  // Requests related to a ProcessingGroup
  /**
   * Get status of a `ProcessingGroup`
   *
   * @param processingGroupId ID of the ProcessingGroup
   */
  def statusProcessingGroup(processingGroupId: ObjectId): Either[String, Option[GroupStatusMessage]] = {
    val res = query(baseReq.GET / "group" / processingGroupId.toString)

    res.right.map { json =>
      Json.parse(json).validate[GroupStatusMessage].fold(
        valid = { groupStatusMsg => Some(groupStatusMsg) },
        invalid = { fieldErrors => logErrorsAndReturnNone(fieldErrors) })
    }
  }

  /**
   * Abort a `ProcessingGroup`
   *
   * @param processingGroupId ID of the ProcessingGroup
   * @param reason Explain why the execution of this ProcessingGroup is aborted
   */
  def abortProcessingGroup(processingGroupId: ObjectId, reason: String): Either[String, Option[String]] = {
    val res = query(baseReq.addQueryParameter("reason", reason).DELETE / "group" / processingGroupId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  /**
   * Resume execution of a `ProcessingGroup` that is on hold
   *
   * @param processingGroupId ID of the ProcessingGroup
   */
  def resumeProcessingGroup(processingGroupId: ObjectId): Either[String, Option[String]] = {
    val res = query(baseReq.PATCH / "group" / processingGroupId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  /**
   * Cleanup intermediate data of a `ProcessingGroup`
   *
   * @param processingGroupId ID of the ProcessingGroup
   */
  def cleanupDataProcessingGroup(processingGroupId: ObjectId): Either[String, Option[String]] = {
    val res = query(baseReq.DELETE / "group/data" / processingGroupId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  // Requests related to a Processing
  /**
   * Submit a new `Processing`
   */
  def submitProcessing(processingMsg: ProcessingMessage): Either[String, Option[ProcessingMessage]] = {
    import HidePasswords._
    Logger.debug(Json.prettyPrint(Json.toJson(processingMsg.hidePassword)))
    val res = query(requestAsJson(baseReq).PUT / "processing" << Json.stringify(Json.toJson(processingMsg)))

    res.right.map { json =>
      Json.parse(json).validate[ProcessingMessage].fold(
        valid = { processingMsg => Some(processingMsg) },
        invalid = { fieldErrors => logErrorsAndReturnNone(fieldErrors) })
    }
  }

  /**
   * Get status of a `Processing`
   *
   * @param processingId ID of the Processing
   */
  def statusProcessing(processingId: ObjectId): Either[String, Option[StatusContainerMessage]] = {
    val res = query(baseReq.GET / "processing" / processingId.toString)

    res.right.map { json =>
      Json.parse(json).validate[StatusContainerMessage].fold(
        valid = { statusContainerMsg => Some(statusContainerMsg) },
        invalid = { fieldErrors => logErrorsAndReturnNone(fieldErrors) })
    }
  }

  /**
   * Abort a `Processing`
   *
   * @param processingId ID of the Processing
   * @param reason Explain why the execution of this Processing is aborted
   */
  def abortProcessing(processingId: ObjectId, reason: String): Either[String, Option[String]] = {
    val res = query(baseReq.addQueryParameter("reason", reason).DELETE / "processing" / processingId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  /**
   * Resume execution of a `Processing` that is on hold
   */
  def resumeProcessing(processingId: ObjectId): Either[String, Option[String]] = {
    val res = query(baseReq.PATCH / "processing" / processingId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  /**
   * Cleanup intermediate data of a `Processing`
   *
   * @param processingId ID of the Processing
   */
  def cleanupDataProcessing(processingId: ObjectId): Either[String, Option[String]] = {
    val res = query(baseReq.DELETE / "processing/data" / processingId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  // Private helper methods
  private def requestAsJson(req: Req) = req.setContentType("application/json", "UTF-8")

  private def query(req: Req) = {
    Logger.debug(s"Processing Manager Query: ${req.url}")
    val res = Http(req OK as.Response(x => x)).either.apply
    val tres = res match {
      case Right(response) => {
        Logger.trace(s"Processing Manager Response:\n" +
          s"# Headers: ${response.getHeaders}\n" +
          s"# ContentType: ${response.getContentType}\n" +
          s"# StatusCode: ${response.getStatusCode}\n" +
          s"# StatusText: ${response.getStatusText}\n" +
          s"# ResponseBody: ${response.getResponseBody}")
        if (response.getStatusCode == 200)
          Right(response.getResponseBody)
        else
          Left(s"Processing Manager service problem (R): ${response.getContentType}")
      }
      case Left(exception) => Left(s"Processing Manager service problem: ${exception.getMessage}")
    }

    // TODO for debuging purposes, remove later
    tres match {
      case Right(r) => Logger.debug(Json.prettyPrint(Json.parse(r)))
      case Left(l)  => Logger.error(l)
    }

    tres
  }

  private def validateActionMsgAndExtractMsg(res: Either[String, String]): Either[String, Option[String]] =
    res.right.map { json =>
      Json.parse(json).validate[ActionMessage].fold(
        valid = { actionMsg => actionMsg.message },
        invalid = { fieldErrors => logErrorsAndReturnNone(fieldErrors) })
    }

  private def logErrorsAndReturnNone(fieldErrors: Seq[(JsPath, Seq[ValidationError])]) = {
    fieldErrors.foreach(x => { Logger.error("field: " + x._1 + ", errors: " + x._2) })
    None
  }
}

object HidePasswords {

  implicit class HideForCredentials(credentials: Option[Credentials]) {
    def hidePassword = credentials.map(_.copy(password = "HiddenFromLog"))
  }

  implicit class HideForInputsOrOutputs(listPortMessagePart: List[PortMessagePart]) {
    def hidePassword = listPortMessagePart.map { portMessagePart =>
      portMessagePart.copy(
        readCreds = portMessagePart.readCreds.hidePassword,
        writeCreds = portMessagePart.writeCreds.hidePassword)
    }
  }

  implicit class HideForProcessingMessage(processingMessage: ProcessingMessage) {
    def hidePassword = processingMessage.copy(
      inputs = processingMessage.inputs.hidePassword,
      outputs = processingMessage.outputs.hidePassword,
      platformCreds = processingMessage.platformCreds.hidePassword)
  }
}
