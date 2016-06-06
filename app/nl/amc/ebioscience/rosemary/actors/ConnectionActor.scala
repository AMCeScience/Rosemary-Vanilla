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
