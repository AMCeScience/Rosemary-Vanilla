package nl.amc.ebioscience.rosemary.actors

import javax.inject._
import com.google.inject.assistedinject.Assisted
import akka.actor.{ Actor, ActorRef, PoisonPill }
import play.api.libs.json.{ JsObject, JsString, JsValue }
import play.api.mvc.Controller
import nl.amc.ebioscience.rosemary.models.User
import nl.amc.ebioscience.rosemary.services.SecurityService
import nl.amc.ebioscience.rosemary.core.WebSockets
import akka.event.LoggingReceive
import akka.actor.ActorLogging

object ConnectionActor {
  trait Factory {
    def apply(out: ActorRef): Actor
  }
}

class ConnectionActor @Inject() (
  @Assisted out: ActorRef,
  securityService: SecurityService)
    extends Actor with Controller with ActorLogging {

  var _user: Option[User.Id] = None
  
  override def preStart(): Unit = {
    super.preStart()

    log.info(s"Created a new ConnectionActor: $self")
  }

  def receive = LoggingReceive {
    case msg: JsObject =>
      log.info("Websocket IN: " + msg)
      (msg \ "kind").get match {
        case JsString("auth") =>
          (msg \ "data").get match {
            case JsString(data) =>
              securityService.getUserFromToken(data) match {
                case Right(user) =>
                  WebSockets.register(user, this); _user = Some(user)
                case Left(_) => case _ => log.error("Unknown websocket user-token"); self ! PoisonPill
              }
            case _ => log.error("Invalid websocket authentication message: " + msg); self ! PoisonPill
          }
        case _ => log.error("Unknown incoming websocket message: " + msg); self ! PoisonPill
      }
  }

  def send(msg: JsValue) = {
    log.info("Websocket OUT: " + msg)
    out ! msg
  }

  override def postStop() = {
    log.info(s"Stopped a ConnectionActor: $self")
    _user.map(WebSockets.deregister(_, this))
  }
}
