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
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.InjectedActorSupport
import akka.event.LoggingReceive

class ConnectionParentActor @Inject() (childFactory: ConnectionActor.Factory)
    extends Actor with InjectedActorSupport with ActorLogging {
  import ConnectionParentActor._

  override def receive: Receive = LoggingReceive {
    case Create(id, out) =>
      val child: ActorRef = injectedChild(childFactory(out), s"connectionActor-$id")
      log.info(s"Created connectionActor-$id")
      sender() ! child
  }
}

object ConnectionParentActor {
  case class Create(id: String, out: ActorRef)
}
