/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */


import akka.actor.{ActorRef, ActorSystem, Props, Status}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.{KillSwitches, Materializer, OverflowStrategy, SharedKillSwitch}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * Define the socket service route
 * @param SAP
 * @param system implicit context
 */
class ChatSocket(SAP:Props)(implicit val system:ActorSystem) {
  //for Flows, make stream blueprint to running stream
  implicit lazy val materializer: Materializer = Materializer(system)

  val socketActorProps: Props = SAP
  val keepAliveMessage: Option[TextMessage] = Some(TextMessage("""{"method":"keepAlive"}"""))
  val keepAliveTimeout: FiniteDuration = 1.minute

  lazy val sinkActorProps: Props = SocketSinkActor.props(socketActorProps)
  lazy val socketsKillSwitch: SharedKillSwitch = KillSwitches.shared("sockets")
  lazy val supervisor: ActorRef = system.actorOf(SocketSinkSupervisor.props(), "sockets")

  /**
   * Define the blueprint of socket actor network
   *  -here, we can change the limit of socket actor amount
   *  -once the server meet the limit, the sever will deny later request
   *  -kind of circuit breaker  pattern
   * @param sinkActor
   * @return
   */
  def socketFlow(sinkActor: ActorRef): Flow[Message, Message, Unit] = {
    val flow: Flow[Message, Message, ActorRef] =
      Flow.fromSinkAndSourceMat(
        Sink.actorRef(sinkActor, Status.Success, Status.Failure),
        Source.actorRef(20, OverflowStrategy.fail)
      )(Keep.right)

    // define the user message flow to socket actor
    val flow2: Flow[Message, Message, Unit] = flow.mapMaterializedValue(
      sourceActor => sinkActor ! sourceActor
    )

    // for maintain the socket, user timeout to limit the actor status
    keepAliveMessage match {
      case Some(message) =>
        flow2.keepAlive(keepAliveTimeout, () => message)
      case None => flow2
    }
  }

  def stop(): Unit ={
    socketsKillSwitch.shutdown()
  }

  def routes: Route =
    path("socket") {
      extractWebSocketUpgrade { _ =>
        onSuccess((supervisor ? sinkActorProps) (1.second).mapTo[ActorRef]) {
          sinkActor: ActorRef =>
//            println("get socket request")
            handleWebSocketMessages(socketFlow(sinkActor).via(socketsKillSwitch.flow))
        }
      }
    }

}

