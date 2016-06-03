package nl.amc.ebioscience.rosemary.controllers.api

import javax.inject._
import play.api.{ Application => PlayApplication, _ }
import play.api.mvc._
import play.api.libs.json._
import scala.reflect.runtime.universe
import nl.amc.ebioscience.rosemary.core.processing._
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.controllers.JsonHelpers
import nl.amc.ebioscience.rosemary.core.{ WebSockets, HelperTools }
import nl.amc.ebioscience.rosemary.core.search.{ SearchReader, SearchWriter, SupportedTypes }
import nl.amc.ebioscience.rosemary.services.SecurityService
import nl.amc.ebioscience.rosemary.services.processing._
import nl.amc.ebioscience.processingmanager.types.messaging.{ ProcessingMessage, PortMessagePart }
import nl.amc.ebioscience.processingmanager.types.{ ProcessingLifeCycle, PortType, Credentials }
import java.util.Date

@Singleton
class ProcessingsController @Inject() (
    securityService: SecurityService,
    processingManagerClient: ProcessingManagerClient,
    processingHelper: ProcessingHelper) extends Controller with JsonHelpers {

  case class SubmitProcessingRequest(
      workspace: Tag.Id,
      application: Recipe.Id,
      description: String,
      dataPorts: Set[SubmitProcessingRequest.DataPort],
      paramPorts: Set[SubmitProcessingRequest.ParamPort]) {

    def validate: Either[String, Map[DefaultModelBase.Id, BaseEntity]] = {

      // check existence of workspace tag id
      val eitherWorkspace = Tag.findOneById(workspace).map {
        w => Right(Map(w.id -> w))
      } getOrElse Left(s"Could not find tag_id $workspace")

      // check existence of application id
      val eitherApplication = Recipe.findOneById(application).map {
        r =>
          r match {
            case a: Application => Right(Map(a.id -> a))
            case _              => Left(s"Recipe with ID $application is not an Application.")
          }
      } getOrElse Left(s"Could not find Recipe with ID: $application")

      // check existence of datum ids
      val dataMapList = for {
        dataPort <- dataPorts.toList
        datum <- Datum.findOneById(dataPort.datum)
      } yield Map(datum.id -> datum)
      val dataMap = dataMapList reduce (_ ++ _)
      val eitherData = for {
        dataPort <- dataPorts.toList
      } yield dataMap get dataPort.datum map {
        d => Right(Map(d.id -> d))
      } getOrElse Left(s"Could not find datum_id ${dataPort.datum}")

      // check existence of port names
      val eitherPorts = eitherApplication.right.flatMap { appIdMap =>
        val appPorts = appIdMap(application).iPorts.map(_.name)
        val reqPorts = (dataPorts.map(_.port) ++ paramPorts.map(_.port)).toSet
        val diffs = appPorts diff reqPorts
        if (diffs.isEmpty) Right(appIdMap) else Left(s"Could not find port numbers: $diffs")
      }

      // concatenate all "Eithers"
      val eithersList = eitherData ::: List(eitherWorkspace, eitherApplication, eitherPorts)
      val eitherIterables = HelperTools.evertEitherList(eithersList)
      // reduce iterables
      eitherIterables match {
        case Right(ml) => Right(ml reduce (_ ++ _))
        case Left(el)  => Left(el mkString (" , "))
      }
    }
  }
  object SubmitProcessingRequest {
    case class DataPort(port: String, datum: Datum.Id)
    case class ParamPort(port: String, param: String)
    implicit val dataPortFmt = Json.format[SubmitProcessingRequest.DataPort]
    implicit val paramPortFmt = Json.format[SubmitProcessingRequest.ParamPort]
    implicit val requestFormatter = Json.format[SubmitProcessingRequest]
  }
  def create = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace(s"Request: $json")
    json.validate[SubmitProcessingRequest].fold(
      valid = { submitReq =>
        submitReq.validate match {
          case Right(objectMap) => // every id exists
            val workspace = objectMap(submitReq.workspace).asInstanceOf[WorkspaceTag]
            val dataProcessingTag = Tag.getProcessingCategory(Tag.ProcessingCategories.DataProcessing.toString)
            val inPreparationStatusTag = Tag.getProcessingStatusTag(ProcessingLifeCycle.InPreparation.toString)
            val abortedStatusTag = Tag.getProcessingStatusTag(ProcessingLifeCycle.Aborted.toString)
            val application = objectMap(submitReq.application).asInstanceOf[Application]

            // run-time binding using the Scala reflection API
            val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
            val module = runtimeMirror.staticModule(application.transformer)
            val transformer = runtimeMirror.reflectModule(module).instance.asInstanceOf[Transformer]

            // to avoid multiple queries to the DB, wrap it in the Cybertronian
            val cybertronian = Cybertronian(
              application = application,
              dataPorts = submitReq.dataPorts.map { dp => (dp.port, Seq(objectMap(dp.datum).asInstanceOf[Datum])) }.toMap,
              paramPorts = submitReq.paramPorts.map { pp => (pp.port, Seq(pp.param)) }.toMap)

            transformer.revealDecepticons(cybertronian) match {
              case Some(map) => Conflict(map mkString (" ; ")) // report domain-specific problems related to the type of inputs
              case None => { // there is no domain-specific problem

                // Raw param and data ports for ProcessingGroup
                val params = submitReq.paramPorts.toSeq.map { pp =>
                  ParamOrDatum(name = pp.port, param = Some(pp.param))
                }
                val data = submitReq.dataPorts.toSeq.map { dp =>
                  ParamOrDatum(name = dp.port, datum = Some(DatumAndReplica(datum = dp.datum)))
                }

                // Create a new ProcessingGroup
                val processingGroup = ProcessingGroup(
                  name = submitReq.description,
                  initiator = User.current.id,
                  inputs = data ++ params,
                  recipes = Set(application.id),
                  tags = Set(workspace.id, dataProcessingTag.id))

                // Define credentials
                val creds = User.credentialFor(transformer.planet.id) orElse {
                  Logger.debug(s"${User.current.email} has no credential for ${transformer.planet.name}, trying community credentials...")
                  for (user <- transformer.planet.username; pass <- transformer.planet.password) yield Credential(
                    resource = transformer.planet.id,
                    username = user,
                    password = pass)
                }
                val pmcreds = creds.map { c =>
                  Credentials(
                    username = c.username,
                    password = c.password,
                    server = "None", // TODO
                    authType = "userpass")
                }

                // Transform the user request to one or more Processings according to the domain-specific definition
                val seqIOICP = transformer.transform(cybertronian)

                // Create a sequence of Processings and ProcessingMessages for each Set of IOInflatedConcretePorts
                val psAndpms = seqIOICP.map { ioicp =>

                  // Create a Processing for each Set of IOInflatedConcretePorts
                  val p = Processing(
                    parentId = Some(processingGroup.id),
                    name = submitReq.description,
                    initiator = User.current.id,
                    inputs = ioicp.inputs.map { icp =>
                      ParamOrDatum(
                        name = icp.name,
                        param = icp.data match {
                          case Param(constant) => Some(constant)
                          case _               => None
                        },
                        datum = icp.data match {
                          case ConcreteDatum(data, replica) => Some(DatumAndReplica(data.id, Some(replica.id)))
                          case _                            => None
                        })
                    },
                    recipes = Set(application.id),
                    tags = Set(workspace.id, dataProcessingTag.id))

                  // Create a ProcessingMessage for each set of IOInflatedConcretePorts and its respective Processing
                  val pm = ProcessingMessage(
                    id = p.id.toString,
                    appName = application.name,
                    appVersion = application.version,
                    appPlatform = application.platform,
                    desc = submitReq.description,
                    inputs = ioicp.inputs.map { icp =>
                      PortMessagePart(
                        portName = icp.name,
                        valueType = icp.data match {
                          case Param(_)            => PortType.Constant.toString
                          case ConcreteDatum(_, _) => PortType.File.toString
                          case _                   => "error: unrecognized type"
                        },
                        value = icp.data match {
                          case Param(constant)           => constant
                          case ConcreteDatum(_, replica) => transformer.planet.uri + "/" + replica.location
                          case _                         => "error: unrecognized value"
                        },
                        resourceName = transformer.planet.name,
                        readCreds = pmcreds,
                        writeCreds = None) // TODO
                    }.toList,
                    outputs = ioicp.outputs.map { icp =>
                      PortMessagePart(
                        portName = icp.name,
                        valueType = icp.data match {
                          case FutureDatum(_) => PortType.File.toString
                          case _              => "error: unrecognized type"
                        },
                        value = icp.data match {
                          case FutureDatum(url) => url
                          case _                => "error: unrecognized value"
                        },
                        resourceName = transformer.planet.name,
                        readCreds = None, // TODO
                        writeCreds = pmcreds)
                    }.toList,
                    groupId = Some(processingGroup.id.toString),
                    platformCreds = None) // TODO

                  (p, pm)
                }

                // Submit ProcessingMessages one by one and update their status accordingly, and save them
                val insertedPs = psAndpms map { pAndpm =>
                  // Submit Processing
                  processingManagerClient.submitProcessing(pAndpm._2) match {
                    case Right(r) => pAndpm._1.copy(progress = 10,
                      statuses = Seq(nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.InPreparation)),
                      tags = pAndpm._1.tags + inPreparationStatusTag.id).insert
                    case Left(e) => pAndpm._1.copy(progress = 0,
                      statuses = Seq(nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.Aborted)),
                      tags = pAndpm._1.tags + abortedStatusTag.id).insert
                  }
                }

                // Update status of the ProcessingGroup and save it
                val avgProgress = insertedPs.map(_.progress).sum / insertedPs.length
                val insertedPG = if (avgProgress > 0)
                  processingGroup.copy(progress = avgProgress,
                    statuses = Seq(nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.InPreparation)),
                    tags = processingGroup.tags + inPreparationStatusTag.id).insert
                else processingGroup.copy(progress = avgProgress,
                  statuses = Seq(nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.Aborted)),
                  tags = processingGroup.tags + abortedStatusTag.id).insert // all failed

                // Index Processings and their ProcessingGroup
                SearchWriter.add(processingGroup)
                insertedPs.foreach(SearchWriter.add(_))
                SearchWriter.commit

                // Create and save user action notification
                val upNotification = UserProcessingNotification(
                  actor = User.current.id,
                  action = "submitted",
                  processing = processingGroup.id,
                  tags = Set(workspace.id),
                  info = Info(dict = Map("countData" -> Valunit(cybertronian.dataPorts.size.toString, Some("Integer"))))).insert

                // Create and save processing status notification
                val pNotification = ProcessingNotification(
                  processing = processingGroup.id,
                  status = insertedPG.lastStatus.get,
                  tags = Set(workspace.id),
                  info = Info(dict = Map("countSubmission" -> Valunit(insertedPs.length.toString, Some("Integer"))))).insert

                // Send notification via WebSocket
                val socket = WebSockets.getSocket
                socket.map(_.send("notification", upNotification.toJson))
                socket.map(_.send("notification", pNotification.toJson))

                // Finally send the new processing to the front-end
                Ok(processingGroup.toJson)
              }
            }
          case Left(errors) => Conflict(errors) // report invalid ids
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  private def extractInputs(inflatedConcretePorts: Set[InflatedConcretePort]): Set[ParamOrDatum] = {
    ???
    //    inflatedConcretePorts.filter(_.data.isInstanceOf[
  }

  case class ProcessingIORequest(input: Option[Datum.Id], output: Option[Datum.Id])
  object ProcessingIORequest {
    implicit val processingIORequestFmt = Json.format[ProcessingIORequest]
  }

  def findByIO = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    json.validate[ProcessingIORequest].fold(
      valid = { req =>
        (req.input, req.output) match {
          case (None, None)       => Conflict(s"At least either of input or output should be provided.")
          case (Some(i), None)    => Ok(Processing.findByI(i).toJson)
          case (None, Some(o))    => Ok(Processing.findByO(o).toJson)
          case (Some(i), Some(o)) => Ok(Processing.findByIorO(i, o).toJson)
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  def queryId(id: Processing.Id) = securityService.HasToken(parse.empty) { implicit request =>
    Processing.findOneById(id).map { processing =>
      Ok(processing.toJson)
    } getOrElse Conflict(s"Could not find Processing with ID: $id")
  }

  def abort(id: Processing.Id) = securityService.HasToken(parse.json) { implicit request =>
    Logger.trace("Request: " + request.body)
    Processing.findOneById(id).map { processing =>
      val reason = (request.body \ "reason").asOpt[String].getOrElse("Yes, We Can!")
      // TODO Send abort request to the Processing Manager
      processingManagerClient.abortProcessing(processing.id, reason).fold(
        { error => Conflict(error) }, // Report Processing Manager service connection problems
        { optMsg => // Call to the Processing Manager service was successful  
          optMsg match {
            case None => Logger.warn(s"Invalid Json response received when aborting the Processing ${processing.id}")
            case Some(msg) => msg match {
              case "OK" =>
                // Update ProcessingGroup status and send notification about its status change
                processingHelper.updateStatusAndSendNotification(processing)
              // TODO Send user action notification
              case m @ _ => Logger.warn(s"Processing Manager says that aborting the Processing ${processing.id} was not OK: $m")
            }
          }
        })
      // Redirect(s"/api/v1/processings/${id}")
      Ok("OK!")
    } getOrElse Conflict(s"Could not find processing with ID: $id")
  }

  def resume(id: Processing.Id) = securityService.HasToken(parse.empty) { implicit request =>
    Processing.findOneById(id).map { processing =>
      // Send resume request to the Processing Manager 
      processingManagerClient.resumeProcessing(processing.id).fold(
        { error => Conflict(error) }, // Report Processing Manager service connection problems
        { optMsg => // Call to the Processing Manager service was successful 
          optMsg match {
            case None => Logger.warn(s"Invalid Json response received when resuming the Processing ${processing.id}")
            case Some(msg) => msg match {
              case "OK" =>
                // Update Processing status and send notification about its status change
                processingHelper.updateStatusAndSendNotification(processing)
              // TODO Send user action notification
              case m @ _ => Logger.warn(s"Processing Manager says that resuming the Processing ${processing.id} was not OK: $m")
            }
          }
        })
      // Redirect(s"/api/v1/processings/${id}")
      Ok("OK!")
    } getOrElse Conflict(s"Could not find processing with ID: $id")
  }
}
