package nl.amc.ebioscience.rosemary.controllers

import javax.inject._
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.streams.ActorFlow
import play.api.mvc.{ Controller, RequestHeader, WebSocket }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.reactivestreams.Publisher
import akka.actor.{ ActorSystem, ActorRef }
import akka.event.Logging
import akka.NotUsed
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout
import nl.amc.ebioscience.rosemary.actors.ConnectionParentActor

/**
 * WebSockets using Akka Actors and Dependency Injection based on this example:
 * https://github.com/playframework/play-websocket-scala
 */
@Singleton
class WebSocketController @Inject() (
  @Named("connectionParentActor") connectionParentActor: ActorRef)(
    implicit actorSystem: ActorSystem,
    mat: Materializer,
    ec: ExecutionContext)
    extends Controller {

  // Use a direct reference to SLF4J
  private val logger = org.slf4j.LoggerFactory.getLogger("controllers.WebSocketController")

  def socket = WebSocket.acceptOrResult[JsValue, JsValue] { requestHeader =>
    wsFutureFlow(requestHeader).map { flow =>
      Right(flow)
    }.recover {
      case e: Exception =>
        logger.error("Cannot create websocket", e)
        val jsError = Json.obj("error" -> "Cannot create websocket")
        val result = InternalServerError(jsError)
        Left(result)
    }
  }

  /**
   * Creates a Future containing a Flow of JsValue in and out.
   */
  private def wsFutureFlow(request: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    // create an actor ref source and associated publisher for sink
    val (webSocketOut: ActorRef, webSocketIn: Publisher[JsValue]) = createWebSocketConnections()

    // Create a user actor off the request id and attach it to the source
    val userActorFuture = createUserActor(request.id.toString, webSocketOut)

    // Once we have an actor available, create a flow...
    userActorFuture.map { userActor =>
      createWebSocketFlow(webSocketIn, userActor)
    }
  }

  /**
   * Creates a materialized flow for the websocket, exposing the source and sink.
   *
   * @return the materialized input and output of the flow.
   */
  private def createWebSocketConnections(): (ActorRef, Publisher[JsValue]) = {

    // Creates a source to be materialized as an actor reference.
    val source: Source[JsValue, ActorRef] = {
      // If you want to log on a flow, you have to use a logging adapter.
      // http://doc.akka.io/docs/akka/2.4.4/scala/logging.html#SLF4J
      val logging = Logging(actorSystem.eventStream, logger.getName)

      // Creating a source can be done through various means, but here we want
      // the source exposed as an actor so we can send it messages from other
      // actors.
      Source.actorRef[JsValue](10, OverflowStrategy.dropTail).log("actorRefSource")(logging)
    }

    // Creates a sink to be materialized as a publisher.  
    // Fanout is false as we only want a single subscriber here.
    val sink: Sink[JsValue, Publisher[JsValue]] = Sink.asPublisher(fanout = false)

    // Connect the source and sink into a flow, telling it to keep the materialized values,
    // and then kicks the flow into existence.
    source.toMat(sink)(Keep.both).run()
  }

  /**
   * Creates a flow of events from the websocket to the user actor.
   *
   * When the flow is terminated, the user actor is no longer needed and is stopped.
   *
   * @param connectionActor   the connection actor receiving websocket events.
   * @param webSocketIn the "read" side of the websocket, that publishes JsValue to ConnectionActor.
   * @return a Flow of JsValue in both directions.
   */
  private def createWebSocketFlow(webSocketIn: Publisher[JsValue], connectionActor: ActorRef): Flow[JsValue, JsValue, NotUsed] = {
    // http://doc.akka.io/docs/akka/current/scala/stream/stream-flows-and-basics.html#stream-materialization
    // http://doc.akka.io/docs/akka/current/scala/stream/stream-integrations.html#integrating-with-actors

    // source is what comes in: browser ws events -> play -> publisher -> userActor
    // sink is what comes out:  userActor -> websocketOut -> play -> browser ws events
    val flow = {
      val sink = Sink.actorRef(connectionActor, akka.actor.Status.Success(()))
      val source = Source.fromPublisher(webSocketIn)
      Flow.fromSinkAndSource(sink, source)
    }

    // Unhook the user actor when the websocket flow terminates
    // http://doc.akka.io/docs/akka/current/scala/stream/stages-overview.html#watchTermination
    val flowWatch: Flow[JsValue, JsValue, NotUsed] = flow.watchTermination() { (_, termination) =>
      termination.foreach { done =>
        logger.info(s"Terminating actor $connectionActor")
        actorSystem.stop(connectionActor)
      }
      NotUsed
    }

    flowWatch
  }

  /**
   * Creates a connection actor with a given name, using the websocket out actor for output.
   *
   * @param name         the name of the connection actor.
   * @param webSocketOut the "write" side of the websocket, that the connection actor sends JsValue to.
   * @return a connection actor for this ws connection.
   */
  private def createUserActor(name: String, webSocketOut: ActorRef): Future[ActorRef] = {
    logger.info(s"Creating actor $name for $webSocketOut")
    // Use guice assisted injection to instantiate and configure the child actor.
    val connectionActorFuture = {
      implicit val timeout = Timeout(100.millis)
      (connectionParentActor ? ConnectionParentActor.Create(name, webSocketOut)).mapTo[ActorRef]
    }
    connectionActorFuture
  }
}
