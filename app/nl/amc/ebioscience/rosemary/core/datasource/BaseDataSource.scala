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

import dispatch._
import dispatch.Defaults._
import nl.amc.ebioscience.rosemary.models._
import play.api.Logger

/** General dispatch setup, methods, and configuration */
abstract class BaseDataSource(resource: Resource) {

  /** Credential of the current user for the resource */
  val userCredential: Option[Credential] = User.credentialFor(resource.id)

  val baseUri: String = resource.uri
  val baseReq: Req = url(baseUri)

  Http.configure(_.setAllowPoolingConnection(true).
    setCompressionEnabled(true).
    setConnectionTimeoutInMs(10000).
    setRequestTimeoutInMs(30000))

  /** with the Req.as method, username and password are only sent if the response handler indicates that */
  protected def auth(req: Req) = userCredential map { cred =>
    req.as(cred.username, cred.password)
  } getOrElse {
    Logger.debug(s"${User.current.email} has no credential for ${resource.name}, trying community credentials...")
    val communityCredential = for (user <- resource.username; pass <- resource.password) yield (user, pass)
    communityCredential match {
      case Some(tup) => req.as(tup._1, tup._2)
      case None => {
        Logger.debug(s"There is no community credential for ${resource.name}, trying without any credential...")
        req
      }
    }
  }
}
