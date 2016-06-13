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
package nl.amc.ebioscience.rosemary.models

import nl.amc.ebioscience.rosemary.models.core._
import nl.amc.ebioscience.rosemary.models.core.ModelBase._
import nl.amc.ebioscience.rosemary.models.core.Implicits._
import play.api.Play

/**
 * Resource captures information about data or computing resources
 *
 * @param name Name of this resource
 * @param kind one of the [[ResourceKind]]s
 * @param protocol Protocol String e.g., http or https
 * @param host Host address
 * @param port Port number
 * @param basePath Base Path for this resource, for example, Some("/my/webdav")
 * @param username System-wide username
 * @param password System-wide password
 * @param tags Workspace tags to capture availability for workspaces (not implemented)
 * @param id ID of this Resource, system provided
 * @param info Captures metadata of this Resource
 */
case class Resource(
    name: String,
    kind: ResourceKind.Value, // to determine the class that should be used to access files and meta-data
    protocol: String,
    host: String,
    port: Int = 80,
    basePath: Option[String] = None,
    username: Option[String] = None,
    password: Option[String] = None,
    // TODO Think about community credentials
    tags: Set[Tag.Id] = Set.empty,
    id: Resource.Id = new Resource.Id,
    info: Info = new Info) extends BaseEntity with WithTags{

  /**
   * @return URI of this resource, e.g., "https://localhost:9000/my/files"
   */
  val uri = {
    val bp = basePath.getOrElse("")
    val uri = port match {
      // Must treat port 80 specially because it's not nice to mention it when the port is 80!
      case 80 => s"$protocol://$host$bp"
      case _  => s"$protocol://$host:$port$bp"
    }
    uri
  }
}

/**
 * Resource companion object that contains database queries specific to the `resources` collection.
 */
object Resource extends DefaultModelBase[Resource]("resources") with TagsQueries[Resource] {
  def findResourceByHostname(hostname: String) = findOne(("host" -> hostname))

  val defaultWebdavHost = Play.current.configuration.getString("webdav.host.default").getOrElse("orange.ebioscience.amc.nl")
  /** Helper method to get a single WebDAV resource instance */
  def getDefaultWebdavInstance = findOne(("kind" -> ResourceKind.Webdav.toString, "host" -> defaultWebdavHost)).get
  def getLocalMongoResource = findOne(("kind" -> ResourceKind.Mongodb.toString)).get
}

object ResourceKind extends Enumeration {
  val Webdav, Mongodb = Value
}
