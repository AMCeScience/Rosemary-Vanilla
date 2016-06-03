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
