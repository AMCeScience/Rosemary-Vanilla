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
package nl.amc.ebioscience.rosemary.core.datasource

import nl.amc.ebioscience.rosemary.models._
import nl.amc.ebioscience.rosemary.core.WebSockets.Socket
import com.github.sardine.SardineFactory
import play.api.Logger
import play.api.libs.json._
import scala.collection.JavaConverters._
import com.github.sardine.DavResource
import nl.amc.ebioscience.rosemary.services.CryptoService

/**
 * @param resource The WebDAV resource to connect to
 * @param socket The WebSocket to send the notifications
 * @param importId The ImportID to include in the notifications
 */
class Webdav(
    val resource: Resource,
    socket: Option[Socket] = None,
    importId: Option[String] = None)(
        implicit cryptoService: CryptoService) {

  /** Credential of the current user for the resource */
  private val userCredential: Option[Credential] = User.credentialFor(resource.id)

  val baseUri: String = resource.uri

  private val sardine = userCredential map { cred =>
    SardineFactory.begin(cred.username, cryptoService.decrypt(cred.password))
  } getOrElse {
    Logger.debug(s"${User.current.email} has no credential for ${resource.name}, trying community credentials...")
    val communityCredential = for (user <- resource.username; pass <- resource.password) yield (user, cryptoService.decrypt(pass))
    communityCredential match {
      case Some(tup) => SardineFactory.begin(tup._1, tup._2)
      case None => {
        Logger.debug(s"There is no community credential for ${resource.name}, trying without any credential...")
        SardineFactory.begin
      }
    }
  }
  sardine.enableCompression

  def replicate(dirs: List[String], filename: String, bytes: Array[Byte]) =
    try {
      val dirsStr = dirs mkString ("/")
      Logger.trace(s"Creating directories: $dirsStr")
      createAllDirs(dirs)
      val url = s"${baseUri}/${dirsStr}/${filename}"
      Logger.debug(s"Uploading ${bytes.length} bytes to $url")
      sardine.put(url, bytes)
      socket.map(_.send("import", Json.obj("id" -> importId, "type" -> "Replica", "state" -> "running")))
      Some(s"${dirsStr}/${filename}")
    } catch {
      case e: Exception => {
        e.printStackTrace
        Logger.error(s"WebDAV service problem: ${e.getMessage}")
        None
      }
    }

  /**
   * @param basePath should not be with leading `/`
   * @param dir should not be with leading `/`
   */
  def createDirUnder(basePath: String, dir: String) = {
    val url = s"${baseUri}/${basePath}/${dir}"
    Logger.debug(s"Creating directory $url")
    try {
      sardine.createDirectory(url)
    } catch {
      case e: Exception => {
        e.printStackTrace
        Logger.error(s"WebDAV service problem: ${e.getMessage}")
      }
    }
  }

  /**
   * @param path should not be with `/` as suffix or prefix
   */
  def getDirectoryList(path: String): List[DavResource] = {
    val url = s"${baseUri}/${path}/"
    Logger.debug(s"Getting Directory Listing for $url")
    try {
      sardine.list(url).asScala.toList
    } catch {
      case e: Exception => {
        e.printStackTrace
        Logger.error(s"WebDAV service problem: ${e.getMessage}")
        Nil
      }
    }
  }

  def move(sourcePath: String, destinationPath: String) = {
    val sourceUrl = s"${baseUri}/${sourcePath}"
    val destinationUrl = s"${baseUri}/${destinationPath}"
    Logger.debug(s"Moving ${sourceUrl} to ${destinationUrl}")
    try {
      sardine.move(sourceUrl, destinationUrl)
    } catch {
      case e: Exception =>
        e.printStackTrace
        Logger.error(s"WebDAV service problem: ${e.getMessage}")
    }
  }

  def copy(sourcePath: String, destinationPath: String) = {
    val sourceUrl = s"${baseUri}/${sourcePath}"
    val destinationUrl = s"${baseUri}/${destinationPath}"
    Logger.debug(s"Copying ${sourceUrl} to ${destinationUrl}")
    try {
      sardine.copy(sourceUrl, destinationUrl)
    } catch {
      case e: Exception =>
        e.printStackTrace
        Logger.error(s"WebDAV service problem: ${e.getMessage}")
    }
  }

  private def createAllDirs(dirs: List[String]): Unit = createAllDirsRec(baseUri, dirs)
  private def createAllDirsRec(base: String, dirs: List[String]): Unit = dirs match {
    case Nil => Logger.trace(s"I assure you, all directories for $base are there!")
    case dir :: subdirs => {
      val url = s"${base}/${dir}"
      Logger.trace(s"Now checking and creating $url")
      if (!sardine.exists(url)) sardine.createDirectory(url)
      createAllDirsRec(s"${url}", subdirs)
    }
  }
}
