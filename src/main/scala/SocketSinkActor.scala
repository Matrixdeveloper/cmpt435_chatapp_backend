/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */



import akka.actor.{Actor, ActorRef, Props, Stash, Terminated, Status}
import akka.event.Logging
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import spray.json._

object SocketSinkActor{
  def props(socketActorProps: Props)(implicit materializer: Materializer):Props =
    Props(new SocketSinkActor(socketActorProps))
}

/**
 * Intermedia socket activity watcher
 *  -separate incoming message type
 *  -manager the socket actor lifecycle
 * @param socketActorProps
 * @param materializer
 */
class SocketSinkActor(socketActorProps: Props)(implicit materializer: Materializer)
  extends Actor with Stash  {

  private val logger = Logging(context.system, this)

//  logger.info("Socket opened. Actor created")


  override def receive: Receive = {
    case sourceActor: ActorRef =>
      val user = context.watch(context.actorOf(socketActorProps, "user"))
      unstashAll()
      context.become {
        case TextMessage.Strict(data) => user ! JsonParser(data)
        //          println(data)
        case BinaryMessage.Strict(_) => // ignore
        case TextMessage.Streamed(stream) => stream.runWith(Sink.ignore)
        case BinaryMessage.Streamed(stream) => stream.runWith(Sink.ignore)
        case msg: JsValue if sender == user => sourceActor ! TextMessage(msg.compactPrint)
        case Terminated(`user`) =>
          logger.info("UserActor terminated. Terminating.")
          sourceActor ! Status.Success(())
          context.stop(self)
        case s @ Status.Success(_)=>
          logger.info("Socket closed. Terminating.")
          sourceActor ! s
          context.stop(self)
        case f @ Status.Failure(cause) =>
          logger.error(cause, "Socket failed. Terminating.")
          sourceActor ! f
          context.stop(self)
        case m =>  //logger.warning("Unsupported message: {}", m.toString)
      }
    case _ => stash()
  }


  override def postStop(): Unit = {
    logger.info("SocketActor killed")
  }
}
