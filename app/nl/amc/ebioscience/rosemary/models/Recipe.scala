package nl.amc.ebioscience.rosemary.models

import com.novus.salat.annotations._
import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import play.api.Logger

@Salat
sealed trait Recipe extends WithTags with WithReplica {
  val name: String
  val description: String
  val valid: Boolean
}

case class Application(
  name: String, // This name is going to be used to group different versions of the application
  description: String,
  version: Option[String] = None,
  platform: Option[String] = None, // TODO convert to resource id
  iPorts: Set[AbstractPort],
  pmApplication: PMApplication,
  transformer: String,
  tags: Set[Tag.Id] = Set.empty, // Workspace tags to define in which workspaces this application can be used
  // TODO: Use "ThemeTags" instead of "WorkspaceTags" to prevent this list to become too big
  replicas: Set[Replica] = Set.empty,
  gitResource: Option[GitResource] = None,
  valid: Boolean = true,
  id: Recipe.Id = new Recipe.Id,
  info: Info = new Info // put the class name for validation and conversion in info
  ) extends Recipe

/**
  * @param iPorts Input ports known to the Processing Manager
  * @param oPorts Output ports known to the Procesing Manager
  */
case class PMApplication(
  iPorts: Set[AbstractPort],
  oPorts: Set[AbstractPort],
  info: Info = new Info // TODO: Think about support person, author, etc. Put them in Info?
  )

case class AbstractPort(
  name: String, // Port ID known to the Processing Manager (only available for PMApplication)
  kind: PortKind.Value, // For example: "data", "param", ( "file" only for PMApplication ports ?) 
  info: Info = new Info // put the default value and requirements in info ?
  )

object PortKind extends Enumeration {
  val Data, File, Param = Value
}

case class Pipeline(
    name: String,
    description: String,
    tags: Set[Tag.Id] = Set.empty,
    replicas: Set[Replica] = Set.empty,
    gitResource: Option[GitResource] = None,
    valid: Boolean = true,
    id: Recipe.Id = new Recipe.Id,
    info: Info = new Info) extends Recipe {
}

case class GitResource(
  url: String,
  branch: String = "master",
  commit: String = "HEAD",
  id: GitResource.Id = new GitResource.Id,
  info: Info = new Info) extends BaseEntity

object GitResource {
  type Id = DefaultModelBase.Id
}

object Recipe extends DefaultModelBase[Recipe]("recipes") with TagsQueries[Recipe] {

  def getApplications = findByType("Application")
  def findByName(name: String) = findOne("name" -> name)
}
