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
 * @param basePath Base Path for this resource, for example, Some("/my/webdav")
 * @param username Community-specific username
 * @param password Community-specific password
 */
case class Resource(
    name: String,
    kind: ResourceKind.Value, // to determine the class that should be used to access files and meta-data
    protocol: String,
    host: String,
    port: Int = 80,
    basePath: Option[String] = None,
    // TODO Think about community credentials
    username: Option[String] = None,
    password: Option[String] = None,
    id: Resource.Id = new Resource.Id,
    info: Info = new Info) extends BaseEntity {

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

object Resource extends DefaultModelBase[Resource]("resources") {
  def findResourceByHostname(hostname: String) = findOne(("host" -> hostname))

  val defaultWebdavHost = Play.current.configuration.getString("webdav.host.default").getOrElse("orange.ebioscience.amc.nl")
  /** Helper method to get a single WebDAV resource instance */
  def getDefaultWebdavInstance = findOne(("kind" -> ResourceKind.Webdav.toString, "host" -> defaultWebdavHost)).get
  def getLocalMongoResource = findOne(("kind" -> ResourceKind.Mongodb.toString)).get
}

object ResourceKind extends Enumeration {
  // val Xnat, Webdav, Irods, Mongodb = Value
  val Webdav, Irods, Mongodb = Value
}
