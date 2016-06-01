package nl.amc.ebioscience.rosemary.core.processing

import dispatch._
import dispatch.Defaults._
import play.api.libs.json._
import play.api.data.validation.ValidationError
import play.api.{ Logger, Play }
import nl.amc.ebioscience.processingmanager.types.messaging.{ ActionMessage, ProcessingMessage, StatusContainerMessage, GroupStatusMessage }
import org.bson.types.ObjectId

object ProcessingManagerClient {

  val configuration = Play.current.configuration
  val baseUri = configuration.getString("processingmanager.uri").getOrElse("http://localhost:8080/RestfulProcessingManager")

  val pmUserId = configuration.getInt("processingmanager.default.userid").getOrElse(1)
  val pmProjectId = configuration.getInt("processingmanager.default.projectid").getOrElse(1)
  val baseReq = url(baseUri)

  Http.configure(_.setAllowPoolingConnection(true).
    setCompressionEnabled(true).
    setConnectionTimeoutInMs(10000).
    setRequestTimeoutInMs(150000))

  // Requests related to a ProcessingGroup
  //  def submitProcessingGroup(processingGroup: PMSubmitRequest): Either[String, Option[PMIds]] = {
  //    Logger.debug(Json.prettyPrint(Json.toJson(processingGroup)))
  //    val res = query(requestAsJson(baseReq).PUT << Json.stringify(Json.toJson(processingGroup)))
  //
  //    res.right.map { json =>
  //      Json.parse(json).validate[PMIds].fold(
  //        valid = { pmIds => Some(pmIds) },
  //        invalid = { fieldErrors => logErrorsAndReturnNone(fieldErrors) })
  //    }
  //  }

  def statusProcessingGroup(processingGroupId: ObjectId): Either[String, Option[GroupStatusMessage]] = {
    val res = query(baseReq.GET / "group" / processingGroupId.toString)

    res.right.map { json =>
      Json.parse(json).validate[GroupStatusMessage].fold(
        valid = { groupStatusMsg => Some(groupStatusMsg) },
        invalid = { fieldErrors => logErrorsAndReturnNone(fieldErrors) })
    }
  }

  def abortProcessingGroup(processingGroupId: ObjectId, reason: String): Either[String, Option[String]] = {
    val res = query(baseReq.addQueryParameter("reason", reason).DELETE / "group" / processingGroupId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  def resumeProcessingGroup(processingGroupId: ObjectId): Either[String, Option[String]] = {
    val res = query(baseReq.PATCH / "group" / processingGroupId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  def cleanupDataProcessingGroup(processingGroupId: ObjectId): Either[String, Option[String]] = {
    val res = query(baseReq.DELETE / "group/data" / processingGroupId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  // Requests related to a Processing
  def submitProcessing(processingMsg: ProcessingMessage): Either[String, Option[ProcessingMessage]] = {
    Logger.debug(Json.prettyPrint(Json.toJson(processingMsg)))
    val res = query(requestAsJson(baseReq).PUT / "processing" << Json.stringify(Json.toJson(processingMsg)))

    res.right.map { json =>
      Json.parse(json).validate[ProcessingMessage].fold(
        valid = { processingMsg => Some(processingMsg) },
        invalid = { fieldErrors => logErrorsAndReturnNone(fieldErrors) })
    }
  }

  def statusProcessing(processingId: ObjectId): Either[String, Option[StatusContainerMessage]] = {
    val res = query(baseReq.GET / "processing" / processingId.toString)

    res.right.map { json =>
      Json.parse(json).validate[StatusContainerMessage].fold(
        valid = { statusContainerMsg => Some(statusContainerMsg) },
        invalid = { fieldErrors => logErrorsAndReturnNone(fieldErrors) })
    }
  }

  def abortProcessing(processingId: ObjectId, reason: String): Either[String, Option[String]] = {
    val res = query(baseReq.addQueryParameter("reason", reason).DELETE / "processing" / processingId.toString)
    validateActionMsgAndExtractMsg(res)
  }

  def resumeProcessing(processingId: ObjectId): Either[String, Option[String]] = {
    val res = query(baseReq.PATCH / "processing" / processingId.toString)
    validateActionMsgAndExtractMsg(res)
  }

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
        Logger.trace(s"Processing Manager Response:\n# Headers: ${response.getHeaders}\n# ContentType: ${response.getContentType}\n# StatusCode: ${response.getStatusCode}\n# StatusText: ${response.getStatusText}\n# ResponseBody: ${response.getResponseBody}")
        if (response.getStatusCode == 200) Right(response.getResponseBody) else Left(s"Processing Manager service problem (R): ${response.getContentType}")
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
