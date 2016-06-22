package nl.amc.ebioscience.rosemary.controllers.nsg

import javax.inject._
import scala.collection.mutable.HashMap
import scala.concurrent.{ Future, ExecutionContext }
import nl.amc.ebioscience.rosemary.controllers.JsonHelpers
import nl.amc.ebioscience.rosemary.core.{ Tools, WebSockets }
import nl.amc.ebioscience.rosemary.core.Tools.Slugify
import nl.amc.ebioscience.rosemary.core.datasource.nsg.Xnat
import nl.amc.ebioscience.rosemary.core.datasource.Webdav
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import nl.amc.ebioscience.rosemary.services.{ SecurityService, CryptoService }
import nl.amc.ebioscience.rosemary.services.search.SearchWriter
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Controller
import java.net.InetAddress

@Singleton
class DataImport @Inject() (
    securityService: SecurityService,
    searchWriter: SearchWriter)(
        implicit cryptoService: CryptoService,
        exec: ExecutionContext) extends Controller with JsonHelpers {

  val importMap = new HashMap[String, Xnat]

  /** returns the list of running imports */
  def index = securityService.HasToken(parse.json) { implicit request =>
    Ok(Json.arr(importMap.map {
      case (id, xnat) => {
        Json.obj(
          "id" -> id,
          "resource" -> xnat.resource.id)
      }
    }))
  }

  /** returns the list of available projects on a XNAT resource */
  def projects = securityService.HasTokenAsync(parse.json) { implicit request =>
    val importId = java.util.UUID.randomUUID().toString
    val socket = WebSockets.getSocket
    val json = request.body
    Logger.trace("Request: " + json)

    (json \ "resourceid").asOpt[Resource.Id].map { resourceid =>
      Resource.findOneById(resourceid) match {
        case Some(resource) => resource.kind match {
          case ResourceKind.Xnat => new Xnat(resource, socket, importId).getLeanProjects.map(_ match {
            case Right(iterator) => Ok(iterator.map(_.datum).toList.sortBy(_.name).toJson)
            case Left(error)     => Conflict(error)
          })
          case _ => Future.successful { Conflict(s"${resource.kind} is not supported for data import yet!") }
        }
        case None => Future.successful { Conflict(s"Could not find resource_id $resourceid") }
      }
    } getOrElse {
      Future.successful { BadRequest(Json.toJson(errorMaker("resourceid", "error.path.missing"))) }
    }
  }

  case class DataRequest(resourceid: Resource.Id, projecturi: String, workspace: Tag.Id)
  object DataRequest {
    implicit val dataRequestFmt = Json.format[DataRequest]
  }

  /** imports information of a XNAT project from a XNAT resource into a workspace */
  def data = securityService.HasToken(parse.json) { implicit request =>
    val importId = java.util.UUID.randomUUID().toString
    val socket = WebSockets.getSocket

    val user = User.current_id.value

    Logger.trace(socket.toString)

    val json = request.body
    Logger.trace("Request: " + json)
    json.validate[DataRequest].fold(
      valid = { dataRequest =>
        Resource.findOneById(dataRequest.resourceid) match {
          case Some(resource) => {
            Tag.findOneById(dataRequest.workspace) match {
              case Some(workspace) => {
                val xnat = new Xnat(resource, socket, importId)

                // Sending the first message for live progress report
                importMap += ((importId, xnat))
                socket.map(_.send("import", Json.obj("id" -> importId, "state" -> "start")))

                // Store SystemTag for import
                val dict = Map(
                  "userid" -> Valunit(value = User.current.id.toString, unit = Some("ObjectID")),
                  "resourceid" -> Valunit(value = resource.id.toString, unit = Some("ObjectID")),
                  "workspaceid" -> Valunit(value = workspace.id.toString, unit = Some("ObjectID")),
                  "projecturi" -> Valunit(value = dataRequest.projecturi, unit = Some("URI")))
                val importTag = SystemTag(
                  name = s"${User.current.name}'s import from ${resource.uri}/${dataRequest.projecturi}",
                  kind = "import",
                  info = Info(dict = dict)).insert.asInstanceOf[SystemTag]

                // Import them all!
                val futureEitherProject = xnat.getProject(dataRequest.projecturi, workspace.id, importTag.id)
                futureEitherProject.recover { eitherProject =>
                  eitherProject match {
                    case e: Exception => Logger.error(e.getMessage)
                  }
                }
                futureEitherProject.map { eitherProject =>
                  Logger.trace("hi! from successful future!")
                  eitherProject match {
                    // Welcome to the future! Now you are inside the future!
                    case Right(project) => {
                      val projectData = project.datumWithChildren
                      val subjects = project.children
                      val subjectsData = subjects.map(_.datumWithChildren)
                      val experiments = subjects.flatMap(_.children)
                      val experimentsData = experiments.map(_.datumWithChildren)
                      val scans = experiments.flatMap(_.children)
                      val scansData = scans.map(_.datumWithChildren)
                      val resources = scans.flatMap(_.children)
                      val resourcesData = resources.map(_.datumWithChildren)
                      // val files = resources.flatMap(_.children)
                      // val filesData = files.map(_.datumWithChildren)
                      val result = subjectsData ++ experimentsData ++ scansData ++ resourcesData + projectData // ++ filesData

                      result.foreach { datum =>
                        // Inserting the new document in the MongoDB
                        datum.insert
                        // Indexing the new documents in Lucene search engine
                        searchWriter.add(datum)
                      }
                      searchWriter.commit

                      // Replicate files from the XNAT to the WebDAV
                      val replicateAll = true
                      var countReplica = 0
                      if (replicateAll) {
                        val webdav = User.current_id.withValue(user) {
                          new Webdav(Resource.getDefaultWebdavInstance, socket, Some(importId))
                        }

                        for (xresource <- resources) {
                          Logger.debug(s"Replicating ${xresource.nameOpt.get}")
                          xresource.datumWithChildren.getReplica(resource.id) map { replica =>
                            Logger.trace(s"Replica found: ${replica.location}")
                            xnat.downloadZip(replica.location) match {
                              case Left(error) => Logger.error(error)
                              case Right(bytes) => {
                                Logger.trace(s"Replica ${replica.location} downloaded, size: ${bytes.length}")
                                val xscan = xresource.parent.get
                                val xexperiment = xscan.parent.get
                                val xsubject = xexperiment.parent.get
                                val xproject = xsubject.parent.get
                                val hostname = InetAddress.getLocalHost.getHostName
                                val dirs = List(hostname.slugify,
                                  workspace.name.slugify.concat("_").concat(workspace.id.toString),
                                  xproject.nameOpt.get.slugify,
                                  xsubject.nameOpt.get.slugify,
                                  xexperiment.nameOpt.get.slugify,
                                  xscan.nameOpt.get.slugify)
                                webdav.replicate(dirs, s"${xresource.nameOpt.get}.zip", bytes) map { newReplicaLocation =>
                                  val newReplica = Replica(resource = webdav.resource.id, location = newReplicaLocation)
                                  val existingReplicas = xresource.datumWithChildren.replicas
                                  xresource.datumWithChildren.copy(replicas = existingReplicas + newReplica).update
                                  countReplica += 1
                                }
                              }
                            }
                          }
                        }
                      }

                      // Sending the last message for live progress report
                      importMap -= importId
                      socket.map(_.send("import", Json.obj("id" -> importId, "state" -> "complete")))

                      // Store and send ImportNotification
                      val stats = Map(
                        "countProjects" -> 1,
                        "countSubjects" -> subjectsData.size,
                        "countExperiments" -> experimentsData.size,
                        "countScans" -> scansData.size,
                        "countResources" -> resourcesData.size, // "countFiles" -> filesData.size,
                        "countReplicas" -> countReplica).map { case (k, v) => (k, Valunit(value = v.toString, unit = Some("Integer"))) }
                      Logger.debug(s"Done with import: ${importId} with statistics: ${stats}")

                      val notification = ImportNotification(
                        actor = user.get,
                        resource = resource.id,
                        workspace = workspace.id,
                        imported = importTag.id,
                        tags = Set(workspace.id),
                        info = Info(dict = stats)).insert

                      socket.map(_.send("notification", notification.toJson))

                      // Store the summary of stats also in the SystemTag for import
                      importTag.copy(info = importTag.info.addToDict(stats)).update
                    }
                    case Left(error) => Conflict(error)
                  }
                } // You are leaving the future! Thank you for time travelling with us!

                Logger.debug(s"Sending back import id: ${importId}")
                Ok(Json.obj("id" -> importId))
              }
              case None => Conflict(s"Could not find workspace_id ${dataRequest.workspace}")
            }
          }
          case None => Conflict(s"Could not find resource_id ${dataRequest.resourceid}")
        }
      },
      invalid = { errors =>
        BadRequest(Json.toJson(errors))
      })
  }
}
