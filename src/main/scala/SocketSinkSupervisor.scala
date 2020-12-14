/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */



import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging
import spray.json.JsonParser.ParsingException
import scala.util.control.NonFatal


/**
 * Define the supervisor strategy for socket actor
 *
 */
object SocketSinkSupervisor{
  def props(): Props = Props(new SocketSinkSupervisor)
}

class SocketSinkSupervisor extends Actor{
  private val logger = Logging(context.system, this)

  override def receive: Receive = {
    case props: Props => sender() ! context.actorOf(props)
  }

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(loggingEnabled = true){
      case p: ParsingException =>
        logger.error(p, "Socket supervisor: unexpected message format . Resuming.")
        SupervisorStrategy.Resume
      case NonFatal(e) =>
        logger.error(e, "Socket supervisor: actor error. Terminating.")
        SupervisorStrategy.Stop
      case _ => SupervisorStrategy.Escalate
    }

}
