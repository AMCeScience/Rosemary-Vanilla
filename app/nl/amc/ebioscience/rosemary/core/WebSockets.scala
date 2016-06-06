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
package nl.amc.ebioscience.rosemary.core

import nl.amc.ebioscience.rosemary.models.User
import scala.collection.mutable.{ HashMap, ArrayBuffer }
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger
import play.api.libs.json.{ JsValue, Json }
import nl.amc.ebioscience.rosemary.actors.ConnectionActor

object WebSockets {

  class Socket(val id: User.Id) {
    val connections = new ArrayBuffer[ConnectionActor]

    def send(kind: String, json: JsValue): Unit = send(Json.obj("kind" -> kind, "data" -> json))

    private def send(data: JsValue) = Future { connections.foreach(_.send(data)) }
  }

  val sockets = new HashMap[User.Id, Socket]

  def getUserId = User.current_id.value

  def getSocket: Option[Socket] = {
    getUserId match {
      case Some(id) => sockets.get(id)
      case _        => Logger.error("No authenticated user found"); None
    }
  }

  /** Returns the sockets for user ids if they exist */
  def getSockets(users: Set[User.Id]): Set[Socket] = users collect sockets

  def register(user: User.Id, conn: ConnectionActor) = {
    Logger.debug("Register new WebSocket connection for user: " + user)

    sockets.synchronized {
      sockets.get(user) match {
        case Some(socket) => socket.connections += conn
        case None => {
          val socket = new Socket(user)
          socket.connections += conn
          sockets += ((user, socket))
        }
      }
    }
  }

  def deregister(user: User.Id, conn: ConnectionActor) = {
    Logger.debug("Deregister WebSocket connection for user: " + user)

    sockets.synchronized {
      sockets.get(user) match {
        case Some(socket) => socket.connections -= conn
        case None         => Logger.error("There are no connections registered for user: " + user)
      }
    }
  }
}
