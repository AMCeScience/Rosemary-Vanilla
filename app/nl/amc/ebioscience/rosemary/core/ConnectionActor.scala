package nl.amc.ebioscience.rosemary.core

import akka.actor.{ Actor, ActorRef, Props, PoisonPill }
import nl.amc.ebioscience.rosemary.models.{ User }
import nl.amc.ebioscience.rosemary.controllers.Security
import play.api.libs.json.{ JsObject, JsString, JsValue }
import play.api.Logger
import play.api.mvc.Controller

object ConnectionActor {
  def props(out: ActorRef) = Props(new ConnectionActor(out))
}

class ConnectionActor(out: ActorRef) extends Actor with Controller with Security {
  var _user: Option[User.Id] = None

  def receive = {
    case msg: JsObject =>
      Logger.trace("Websocket IN: " + msg)
      (msg \ "kind").get match {
        case JsString("auth") =>
          (msg \ "data").get match {
            case JsString(data) =>
              getUserFromToken(data) match {
                case Right(user) =>
                  WebSockets.register(user, this); _user = Some(user)
                case Left(_) => case _ => Logger.error("Unknown websocket user-token"); self ! PoisonPill
              }
            case _ => Logger.error("Invalid websocket authentication message: " + msg); self ! PoisonPill
          }
        case _ => Logger.error("Unknown incoming websocket message: " + msg); self ! PoisonPill
      }
  }

  def send(msg: JsValue) = {
    Logger.trace("Websocket OUT: " + msg)
    out ! msg
  }

  override def postStop() = {
    _user.map(WebSockets.deregister(_, this))
  }
}
