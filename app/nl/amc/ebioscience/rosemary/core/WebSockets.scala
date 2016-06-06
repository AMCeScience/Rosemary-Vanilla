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
