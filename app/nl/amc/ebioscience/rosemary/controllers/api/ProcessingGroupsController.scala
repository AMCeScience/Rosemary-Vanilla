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
package nl.amc.ebioscience.rosemary.controllers.api

import javax.inject._
import play.api.{ Application => PlayApplication, _ }
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.actor.{ Props, PoisonPill }
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.reflect.runtime.universe
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.controllers.JsonHelpers
import nl.amc.ebioscience.rosemary.core.{ WebSockets, HelperTools }
import nl.amc.ebioscience.rosemary.actors.ProcessingStatusCheckActor
import nl.amc.ebioscience.rosemary.services.SecurityService
import nl.amc.ebioscience.rosemary.services.processing._
import nl.amc.ebioscience.rosemary.services.search._
import nl.amc.ebioscience.processingmanager.types.messaging.{ ProcessingMessage, PortMessagePart }
import nl.amc.ebioscience.processingmanager.types.{ ProcessingLifeCycle, PortType, Credentials }
import java.util.Date
import akka.actor.ActorSystem

@Singleton
class ProcessingGroupsController @Inject() (
    securityService: SecurityService,
    processingManagerClient: ProcessingManagerClient,
    processingHelper: ProcessingHelper,
    searchWriter: SearchWriter,
    actorSystem: ActorSystem) extends Controller with JsonHelpers {

  case class SubmitProcessingGroupRequest(
      workspace: Tag.Id,
      application: Recipe.Id,
      description: String,
      dataPorts: Set[SubmitProcessingGroupRequest.DataPort],
      paramPorts: Set[SubmitProcessingGroupRequest.ParamPort]) {

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
        data <- Datum.findByIds(dataPort.data.toSet)
      } yield Map(data.id -> data)
      val dataMap = dataMapList reduce (_ ++ _)
      val eitherData = for {
        dataPort <- dataPorts.toList
        dataId <- dataPort.data.toList
      } yield dataMap get dataId map {
        d => Right(Map(d.id -> d))
      } getOrElse Left(s"Could not find datum_id ${dataId}")

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
  object SubmitProcessingGroupRequest {
    case class DataPort(port: String, data: Seq[Datum.Id])
    case class ParamPort(port: String, params: Seq[String])
    implicit val dataPortFmt = Json.format[SubmitProcessingGroupRequest.DataPort]
    implicit val paramPortFmt = Json.format[SubmitProcessingGroupRequest.ParamPort]
    implicit val submitRequestFmt = Json.format[SubmitProcessingGroupRequest]
  }

  /**
   * Submits a new processing, because processing manager replies almost instantly,
   * this method is not Asynchronous
   */
  def create = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[SubmitProcessingGroupRequest].fold(
      valid = { submitRequest =>
        submitRequest.validate match {
          case Right(objectMap) => // every id exist
            val workspace = objectMap(submitRequest.workspace).asInstanceOf[WorkspaceTag]
            val dataProcessingTag = Tag.getProcessingCategory(Tag.ProcessingCategories.DataProcessing.toString)
            val inPreparationStatusTag = Tag.getProcessingStatusTag(ProcessingLifeCycle.InPreparation.toString)
            val abortedStatusTag = Tag.getProcessingStatusTag(ProcessingLifeCycle.Aborted.toString)
            val application = objectMap(submitRequest.application).asInstanceOf[Application]

            // run-time binding using the Scala reflection API
            val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
            val module = runtimeMirror.staticModule(application.transformer)
            val transformer = runtimeMirror.reflectModule(module).instance.asInstanceOf[Transformer]

            // to avoid multiple queries to the DB, wrap it in the Cybertronian
            val cybertronian = Cybertronian(
              application = application,
              dataPorts = submitRequest.dataPorts.map { dp => (dp.port, dp.data.map(objectMap(_).asInstanceOf[Datum])) }.toMap,
              paramPorts = submitRequest.paramPorts.map { pp => (pp.port, pp.params) }.toMap)

            transformer.revealDecepticons(cybertronian) match {
              case Some(map) => Conflict(map mkString (" ; ")) // report domain-specific problems related to the type of inputs
              case None => { // there is no domain-specific problem

                // Raw param and data ports for ProcessingGroup
                val params = submitRequest.paramPorts.toSeq.map { pp =>
                  for (p <- pp.params)
                    yield ParamOrDatum(name = pp.port, param = Some(p))
                }.flatten
                val data = submitRequest.dataPorts.toSeq.map { dp =>
                  for (d <- dp.data)
                    yield ParamOrDatum(name = dp.port, datum = Some(DatumAndReplica(datum = d)))
                }.flatten

                // Create a new ProcessingGroup
                val processingGroup = ProcessingGroup(
                  name = submitRequest.description,
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

                  // Create a sensible name for this Processing
                  val stemData = ioicp.stems.map(_.data).filter(_.isInstanceOf[OnlyDatum]).map(_.asInstanceOf[OnlyDatum]).map(_.datum.name)
                  val inputData = ioicp.inputs.map(_.data).filter(_.isInstanceOf[ConcreteDatum]).map(_.asInstanceOf[ConcreteDatum]).map(_.datum.name)
                  val procName = submitRequest.description + " : " + stemData.mkString(" & ") + " : " + inputData.mkString(" & ")

                  // Create a Processing for each Set of IOInflatedConcretePorts
                  val p = Processing(
                    parentId = Some(processingGroup.id),
                    name = procName,
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
                    desc = procName,
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
                    case Left(e) => pAndpm._1.copy(progress = 5,
                      statuses = Seq(nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.Aborted)),
                      tags = pAndpm._1.tags + abortedStatusTag.id).insert
                  }
                }

                // Update status of the ProcessingGroup and save it
                val avgProgress = insertedPs.map(_.progress).sum / insertedPs.length
                val insertedPG = if (avgProgress > 5)
                  processingGroup.copy(progress = avgProgress,
                    statuses = Seq(nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.InPreparation)),
                    tags = processingGroup.tags + inPreparationStatusTag.id).insert
                else processingGroup.copy(progress = avgProgress,
                  statuses = Seq(nl.amc.ebioscience.rosemary.models.Status(ProcessingLifeCycle.Aborted)),
                  tags = processingGroup.tags + abortedStatusTag.id).insert // all failed

                // Index Processings and their ProcessingGroup
                searchWriter.add(processingGroup)
                insertedPs.foreach(searchWriter.add(_))
                searchWriter.commit

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
        errors => BadRequest(Json.toJson(errors)) // report invalid request
      })
  }

  case class ProcessingGroupIORequest(input: Option[Datum.Id], output: Option[Datum.Id])
  object ProcessingGroupIORequest {
    implicit val processingGroupIORequestFmt = Json.format[ProcessingGroupIORequest]
  }

  def findByIO = securityService.HasToken(parse.json) { implicit request =>
    val json = request.body
    json.validate[ProcessingGroupIORequest].fold(
      valid = { req =>
        (req.input, req.output) match {
          case (None, None)       => Conflict(s"At least either of input or output should be provided.")
          case (Some(i), None)    => Ok(ProcessingGroup.findByI(i).toJson)
          case (None, Some(o))    => Ok(ProcessingGroup.findByO(o).toJson)
          case (Some(i), Some(o)) => Ok(ProcessingGroup.findByIorO(i, o).toJson)
        }
      },
      invalid = {
        errors => BadRequest(Json.toJson(errors))
      })
  }

  def update = Action.async {
    val pmActor = actorSystem.actorOf(Props[ProcessingStatusCheckActor])
    implicit val timeout = Timeout(5.minutes) // needed for `?` below
    val future = (pmActor ? "go for it!").mapTo[String]
    future.map { msg =>
      Logger.debug("going to send the poison pill!")
      pmActor ! PoisonPill
      Ok(msg)
    }
  }

  def queryId(id: ProcessingGroup.Id) = securityService.HasToken(parse.empty) { implicit request =>
    ProcessingGroup.findOneById(id).map { processingGroup =>
      Ok(processingGroup.toJson)
    } getOrElse Conflict(s"Could not find ProcessingGroup ID: $id")
  }

  def queryIds = securityService.HasToken(parse.json) { implicit request =>
    Logger.trace(s"Request: ${request.body}")
    (request.body \ "ids").asOpt[Set[ProcessingGroup.Id]].map { ids =>
      Ok(ProcessingGroup.findByIds(ids).toJson)
    } getOrElse BadRequest(Json.toJson(errorMaker("ids", "error.path.missing")))
  }

  /**
   * Return only the Processings under a ProcessingGroup
   */
  def children(id: ProcessingGroup.Id) = securityService.HasToken(parse.empty) { implicit request =>
    ProcessingGroup.findOneById(id).map { processingGroup =>
      Ok(processingGroup.processings.toJson)
    } getOrElse Conflict(s"Could not find ProcessingGroup ID: $id")
  }

  def abort(id: ProcessingGroup.Id) = securityService.HasToken(parse.json) { implicit request =>
    Logger.trace("Request: " + request.body)
    ProcessingGroup.findOneById(id).map { processingGroup =>
      val reason = (request.body \ "reason").asOpt[String].getOrElse("Yes, We Can!")
      // TODO Send abort request to the Processing Manager
      processingManagerClient.abortProcessingGroup(processingGroup.id, reason).fold(
        { error => Conflict(error) }, // Report Processing Manager service connection problems
        { optMsg => // Call to the Processing Manager service was successful  
          optMsg match {
            case None => Logger.warn(s"Invalid Json response received when aborting the ProcessingGroup ${processingGroup.id}")
            case Some(msg) => msg match {
              case "OK" =>
                // Update ProcessingGroup status and send notification about its status change
                processingHelper.updateStatusAndSendNotification(processingGroup)
              // TODO Send user action notification
              case m @ _ => Logger.warn(s"Processing Manager says that aborting the ProcessingGroup ${processingGroup.id} was not OK: $m")
            }
          }
        })
      //      Redirect(s"/api/v1/processing-groups/${id}")
      Ok("OK!")
    } getOrElse Conflict(s"Could not find processingGroup with ID: $id")
  }

  def resume(id: ProcessingGroup.Id) = securityService.HasToken(parse.empty) { implicit request =>
    ProcessingGroup.findOneById(id).map { processingGroup =>
      // Send resume request to the Processing Manager 
      processingManagerClient.resumeProcessingGroup(processingGroup.id).fold(
        { error => Conflict(error) }, // Report Processing Manager service connection problems
        { optMsg => // Call to the Processing Manager service was successful 
          optMsg match {
            case None => Logger.warn(s"Invalid Json response received when resuming the ProcessingGroup ${processingGroup.id}")
            case Some(msg) => msg match {
              case "OK" =>
                // Update ProcessingGroup status and send notification about its status change
                processingHelper.updateStatusAndSendNotification(processingGroup)
              // TODO Send user action notification
              case m @ _ => Logger.warn(s"Processing Manager says that resuming the ProcessingGroup ${processingGroup.id} was not OK: $m")
            }
          }
        })
      //      Redirect(s"/api/v1/processing-groups/${id}")
      Ok("OK!")
    } getOrElse Conflict(s"Could not find processingGroup with ID: $id")
  }

  //  private def processingGroupAndItsProcessingsToJson(processingGroup: ProcessingGroup) =
  //    Json.obj("processingGroup" -> processingGroup.toJson,
  //      "processings" -> processingGroup.processings.toJson)
}