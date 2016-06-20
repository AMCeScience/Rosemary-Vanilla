package nl.amc.ebioscience.rosemary.core.datasource.nsg

import dispatch._
import dispatch.Defaults._
import com.fasterxml.jackson.databind.JsonNode
import scala.collection.JavaConversions.{ asScalaIterator, iterableAsScalaIterable }
import nl.amc.ebioscience.rosemary.core.{ JJson, HelperTools }
import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.core.datasource.BaseDataSource
import nl.amc.ebioscience.rosemary.core.WebSockets.Socket
import nl.amc.ebioscience.rosemary.core.Tools
import nl.amc.ebioscience.rosemary.services.CryptoService
import play.api.Logger
import play.api.libs.json._

class Xnat(val resource: Resource, socket: Option[Socket], importId: String)(
  implicit cryptoService: CryptoService)
    extends BaseDataSource(resource) {

  lazy val sessionIdPost =
    Http((auth(baseReq).POST / "data" / "JSESSION") OK as.String).option.apply // apply method guarantees that session_id is realized

  lazy val sessionIdFromCookie = {
    Http((auth(baseReq).POST / "data" / "JSESSION") > as.Response(x => x)).option.apply.flatMap { response =>
      response.getHeader("Set-Cookie").split(';').find(str => str.startsWith("JSESSIONID")).map(_.split('=')(1))
    }
  }

  /** Adds the XNAT session ID to the request */
  private def session(req: Req) = if (sessionIdPost.isDefined) {
    Logger.trace(s"XNAT Session ID (Post) = ${sessionIdPost.get}")
    req.GET.addHeader("Cookie", "JSESSIONID=" + sessionIdPost.get)
  } else if (sessionIdFromCookie.isDefined) {
    Logger.trace(s"XNAT Session ID (Get) = ${sessionIdFromCookie.get}")
    req.GET.addHeader("Cookie", "JSESSIONID=" + sessionIdFromCookie.get)
  } else req

  private def json(req: Req) = req.addQueryParameter("format", "json")

  /** Query the XNAT, it uses the JSON API and attaches the authenticated session to the query */
  private def query(req: Req) = {
    val queryReq = json(session(req))
    val fres = Tools.query(queryReq)
    fres.map(res =>
      res match {
        case Right(str) =>
          val node = Json.parse(str)
          Logger.debug("Query Result:" + Json.prettyPrint(node))
          Right(node)
        case Left(exc) => Left(s"XNAT service problem: ${exc.getMessage}")
      })
  }

  trait JsonProcessor {

    protected def nodeToListKeyVal(node: JsValue, prefix: Option[String]) = {
      socket.map(_.send("import", Json.obj("id" -> importId, "type" -> prefix, "state" -> "running")))
      Logger.debug(Json.prettyPrint(node))
      var result: List[(String, String)] = Nil

      node.as[JsObject].fields.foreach(entry => {
        val key = entry._1
        if (!key.startsWith("xnat_")) {
          val value = entry._2.as[String].trim
          if (!value.isEmpty())
            result = (prefix.getOrElse("") + "/" + key, value) :: result
        }
      })
      result
    }

    /** Process the content of (future_)data and generates a list of tuples for meta-data */
    protected def process(data: JsValue, prefix: String): List[(String, Valunit)] = {

      socket.map(_.send("import", Json.obj("id" -> importId, "type" -> prefix, "state" -> "running")))
      Logger.debug(Json.prettyPrint(data))

      var result: List[(String, Valunit)] = Nil

      /**
       * To extract information out of the following construct:
       * {{{
       * data_fields: {
       * 			field: "value"
       * 		name: "key"
       * }
       * }}}
       */
      def dataFieldToKeyVal(node: JsValue) = {
        val value = (node \ "field").as[String].trim
        if (!value.isEmpty()) {
          val key = (node \ "name").as[String]
          result = (key, Valunit(value = value)) :: result
        }
      }

      def dataFieldToListKeyVal(node: JsValue, prefix: Option[String]) = {
        node.as[JsObject].fields.foreach(entry => {
          val key = entry._1
          if (!key.startsWith("xnat_")) {
            val value = entry._2.as[String].trim
            if (!value.isEmpty())
              result = (prefix.getOrElse("") + key, Valunit(value = value)) :: result
          }
        })
      }

      /**
       * To process the custom fields stored as the following:
       * {{{
       * children: [
       * 	{
       * 		field: "fields/field"
       * 		items: [ {
       * 		children: [ ]
       * 		data_fields: {
       * 			field: "value"
       * 		name: "key"
       * 		}
       * 	meta: { }
       * 	} ]
       * }
       * ]
       * }}}
       */
      def customFields(node: JsValue) = {
        (node \ "items").as[JsArray].value.foreach(item =>
          if (!(item \ "meta" \ "isHistory").as[Boolean]) dataFieldToKeyVal((item \ "data_fields").get))
      }

      def parseItemNode(node: JsValue) = {
        // each item node has: children[field, items], data_fields, meta
        if ((node \ "meta" \ "isHistory").as[Boolean]) Nil else {
          dataFieldToListKeyVal((node \ "data_fields").get, None)
        }
      }

      try {
        // get the first element in the items because the rest are only for history (meta:isHistory:true)
        (data \ "items")(0).as[JsObject].fields.foreach(first => {
          first._1 match {
            case "children" => {
              first._2.as[JsArray].value.foreach(second => { // iterate through children
                (second \ "field").as[String] match {
                  case "fields/field" => customFields(second)
                  case prefix @ ("demographics" | "PI") =>
                    (second \ "items").as[JsArray].value.foreach(item => dataFieldToListKeyVal((item \ "data_fields").get, Some(s"$prefix/")))
                  case field => Logger.warn("Unknown field: " + field)
                }
              })
            }
            case "data_fields" => dataFieldToListKeyVal(first._2, None)
            case "meta"        => Logger.trace("meta: isHistory = " + (first._2 \ "isHistory"))
            case part          => Logger.warn("Unknown item-part: " + part)
          }
        })
      } catch {
        case e: Throwable => Logger.error("Invalid JSON", e)
      }

      result.map { case (k, v) => (s"$prefix/$k", v) }
    }

  }

  def getLeanProjects: Future[Either[String, Set[LeanDataNode]]] =
    query(baseReq / "data" / "archive" / "projects").right.map(json =>
      (json \ "ResultSet" \ "Result").as[JsArray].value.map(project =>
        new LeanProject(project)).toSet)

  def getProject(projectUri: String, workspaceId: Tag.Id, importTagId: Tag.Id) = {
    Logger.debug(s"Getting project: $projectUri")
    query(baseReq.setUrl(baseUri + projectUri)).right.map(json =>
      new Project((json \ "items")(0).get, projectUri, workspaceId, importTagId))
  }

  private def getSubjects(project: Project, tagIds: Set[Tag.Id]): Future[Either[String, Set[DataNode]]] = {
    Logger.debug(s"Getting subjects for: ${project.id}")
    query(baseReq / "data" / "archive" / "projects" / project.id.get / "subjects").right.map(json =>
      (json \ "ResultSet" \ "Result").as[JsArray].value.map(subject =>
        new Subject(subject, project, tagIds)).toSet)
  }

  private def getExperiments(project: Project, subject: Subject, tagIds: Set[Tag.Id]): Future[Either[String, Set[DataNode]]] = {
    Logger.debug(s"Getting experiments for: ${subject.id}")
    query(baseReq / "data" / "archive" / "projects" / subject.project.id.get / "subjects" / subject.id.get / "experiments").right.map(json =>
      (json \ "ResultSet" \ "Result").as[JsArray].value.map(experiment =>
        new Experiment(experiment, subject, tagIds)).toSet)
  }

  private def getScans(experiment: Experiment, tagIds: Set[Tag.Id]): Future[Either[String, Set[DataNode]]] = {
    Logger.debug(s"Getting scans for: ${experiment.id}")
    query(baseReq / "data" / "archive" / "experiments" / experiment.id.get / "scans").right.map(json =>
      (json \ "ResultSet" \ "Result").as[JsArray].value.map(scan =>
        new Scan(scan, experiment, tagIds)).toSet)
  }

  private def getResources(scan: Scan, tagIds: Set[Tag.Id]): Future[Either[String, Set[DataNode]]] = {
    val experiment = scan.parent.get.asInstanceOf[Experiment]
    Logger.debug(s"Getting resources for: ${experiment.id}/${scan.id}")
    query(baseReq / "data" / "archive" / "experiments" / experiment.id.get / "scans" / scan.id.get / "resources").right.map(json =>
      (json \ "ResultSet" \ "Result").as[JsArray].value.map(resource =>
        new Resource(resource, scan, tagIds)).toSet)
  }

  private def getFiles(resource: Resource, tagIds: Set[Tag.Id]): Future[Either[String, Set[DataNode]]] = {
    val scan = resource.parent.get.asInstanceOf[Scan]
    val experiment = scan.parent.get.asInstanceOf[Experiment]
    Logger.debug(s"Getting files for: ${experiment.id}/${scan.id}/${resource.id}")
    query(baseReq / "data" / "archive" / "experiments" / experiment.id.get / "scans" / scan.id.get / "resources" / resource.id.get / "files").right.map(json =>
      (json \ "ResultSet" \ "Result").as[JsArray].value.map(file =>
        new File(file, resource, tagIds)).toSet)
  }

  def downloadZip(xnatFilePath: String) = {
    val queryReq = session(url(s"${baseUri}/${xnatFilePath}"))
    Logger.debug(s"Download request: ${queryReq.url}")
    val res = Http(queryReq OK as.Bytes).either.apply()
    for (exc <- res.left) yield "XNAT service problem: " + exc.getMessage
  }

  /**
   * @param node parsed JSON node
   * @param kind DatumCategory
   */
  abstract class LeanDataNode(node: JsValue, kind: Tag.DatumCategories.Value) {
    Logger.debug(s"Initializing a new $kind in LeanDataNode.")
    Logger.debug(Json.prettyPrint(node))

    /**
     * This is to accommodate the differences between nodes coming from ResultSet.Result and Items
     * nodes from ResultSet.Result are flat
     * nodes from Items have the same information under data_fields
     */
    val dataFields = (node \ "data_fields").toOption
    val essentials = if (dataFields.nonEmpty) dataFields.get else node

    /** uri is used to query information for this DataNode */
    val uri: Option[String] = (essentials \ "URI").asOpt[String]
    val id: Option[String] = (essentials \ "ID").asOpt[String]
    val name: String = (essentials \ "name").asOpt[String].getOrElse((essentials \ "label").as[String])

    val categoryTag = Tag.getDatumCategory(kind.toString).id

    lazy val datum = new Datum(
      name = name,
      resource = Some(resource.id),
      idOnResource = id,
      pathOnResource = uri,
      // creator = User.current.id,
      tags = Set(categoryTag))
  }

  /**
   *  @param node parsed JSON node
   *  @param kind DatumCategory
   *  @param parent parent DataNode
   *  @param tagIds WorkspaceTag and ImportTag
   */
  abstract class DataNode(node: JsValue, val kind: Tag.DatumCategories.Value, val parent: Option[DataNode], tagIds: Set[Tag.Id])
      extends LeanDataNode(node, kind) with JsonProcessor {

    Logger.debug(s"Initializing a new $kind in DataNode.")
    Logger.debug(Json.prettyPrint(node))

    /** Get the meta-data of the current DataNode and fill the (future_)data */
    lazy val futureEitherData = query(baseReq.setUrl(baseUri + uri))
    lazy val eitherData = futureEitherData() // this calls the apply method of dispatch enriched future, which means data is there
    lazy val data = eitherData match {
      case Right(jsonNode) => jsonNode
      case Left(error) => {
        Logger.error(error)
        JsNull // empty data
      }
    }

    // Meta-data
    lazy val singleMetadata = process(data, kind.toString)
    lazy val metadata: List[(String, Valunit)] = parent match { // TODO use inheritedDict instead of this
      case Some(parent) => singleMetadata ++ parent.metadata
      case None         => singleMetadata
    }

    // Child nodes
    val futureEitherChildren: Future[Either[String, Set[DataNode]]]
    lazy val eitherChildren = futureEitherChildren() // this calls the apply method of dispatch enriched future, which means data is there
    lazy val children: Set[DataNode] = eitherChildren match {
      case Right(set) => {
        set
      }
      case Left(error) => {
        Logger.error(error)
        Set.empty
      }
    }
    val datumWithChildren: Datum

    // Datum for storage in the database
    override lazy val datum = new Datum(
      name = name,
      resource = Some(resource.id),
      idOnResource = id,
      pathOnResource = uri,
      // creator = User.current.id,
      tags = tagIds + categoryTag,
      info = new Info(metadata.toMap))
  }

  case class LeanProject(node: JsValue)
      extends LeanDataNode(node, Tag.DatumCategories.Project) {

    override val name: String = (node \ "name").as[String]
  }

  /** XNAT Project */
  case class Project(node: JsValue, path: String, workspaceId: Tag.Id, importTagId: Tag.Id)
      extends DataNode(node, Tag.DatumCategories.Project, None, Set(workspaceId, importTagId)) {

    override val uri = Some(path)
    override val name = (node \ "name").as[String]
    lazy val futureEitherChildren = getSubjects(this, Set(workspaceId, importTagId))
    lazy val datumWithChildren = datum.copy(children = children.map(_.datum.id))
  }

  /** XNAT Subject */
  case class Subject(node: JsValue, project: Project, tagIds: Set[Tag.Id])
      extends DataNode(node, Tag.DatumCategories.Subject, Some(project), tagIds) {

    lazy val futureEitherChildren = getExperiments(project, this, tagIds)
    lazy val datumWithChildren = datum.copy(children = children.map(_.datum.id))
  }

  /** XNAT Experiment (Image Session) */
  case class Experiment(node: JsValue, subject: Subject, tagIds: Set[Tag.Id])
      extends DataNode(node, Tag.DatumCategories.Experiment, Some(subject), tagIds) {

    lazy val project = subject.parent.get.asInstanceOf[Project]

    lazy val replica = Replica(resource = resource.id,
      location = s"data/archive/projects/${project.id}/subjects/${subject.id}/experiments/${id}/scans/ALL/files?format=zip")
    lazy val futureEitherChildren = getScans(this, tagIds)
    lazy val datumWithChildren = datum.copy(children = children.map(_.datum.id), replicas = Set(replica))
  }

  /** XNAT Scan */
  case class Scan(node: JsValue, experiment: Experiment, tagIds: Set[Tag.Id])
      extends DataNode(node, Tag.DatumCategories.Scan, Some(experiment), tagIds) {

    lazy val project = subject.parent.get.asInstanceOf[Project]
    lazy val subject = experiment.parent.get.asInstanceOf[Subject]

    override val name = s"${experiment.id}_${id}"
    lazy val replica = Replica(resource = resource.id,
      location = s"data/archive/projects/${project.id}/subjects/${subject.id}/experiments/${experiment.id}/scans/${id}/files?format=zip")
    lazy val futureEitherChildren = getResources(this, tagIds)
    lazy val datumWithChildren = datum.copy(children = children.map(_.datum.id), replicas = Set(replica))
  }

  /** XNAT Resource */
  case class Resource(node: JsValue, scan: Scan, tagIds: Set[Tag.Id])
      extends DataNode(node, Tag.DatumCategories.Resource, Some(scan), tagIds) {

    lazy val project = subject.parent.get.asInstanceOf[Project]
    lazy val subject = experiment.parent.get.asInstanceOf[Subject]
    lazy val experiment = scan.parent.get.asInstanceOf[Experiment]

    override val id = (essentials \ "xnat_abstractresource_id").asOpt[String]
    val label = (essentials \ "label").as[String]
    override val name = s"${scan.name}_$label"
    override val uri = Some(s"data/archive/experiments/${experiment.id}/scans/${scan.id}/resources/${id}")
    import ValunitConvertors._
    override lazy val singleMetadata = nodeToListKeyVal(essentials, Some(kind.toString)).toValunit
    lazy val replica = Replica(resource = resource.id,
      location = s"data/archive/projects/${project.id}/subjects/${subject.id}/experiments/${experiment.id}/scans/${scan.id}/resources/${id}/files?format=zip")
    lazy val futureEitherChildren = Future { Right(Set.empty[DataNode]) }
    // lazy val futureEitherChildren = getFiles(this, tagIds)
    lazy val datumWithChildren = datum.copy(replicas = Set(replica))
    // lazy val datumWithChildren = datum.copy(children = children.map(_.datum.id), replicas = Set(replica))
  }

  /** XNAT File */
  case class File(node: JsValue, xnatResource: Resource, tagIds: Set[Tag.Id])
      extends DataNode(node, Tag.DatumCategories.File, Some(xnatResource), tagIds) {

    override val name = (node \ "name").as[String]
    override val id = Some(name)
    import ValunitConvertors._
    override lazy val singleMetadata = nodeToListKeyVal(essentials, Some(kind.toString)).toValunit
    lazy val replica = Replica(resource = resource.id, location = uri.get)
    lazy val futureEitherChildren = Future { Right(Set.empty[DataNode]) }
    lazy val datumWithChildren = datum
  }
}
