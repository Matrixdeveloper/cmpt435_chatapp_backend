/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */


import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props}
import akka.actor.SupervisorStrategy._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteResult.routeToFlow

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}


/**
 * The Main supervisor for this server
 * @param host server host address
 * @param restPort rest server port number
 * @param socketPort socket server port number
 * @param name name of this sever
 * @param system implicit context
 */
class Supervisor(host:String, restPort:Int, socketPort:Int,name:String)(implicit system: ActorSystem)
  extends Actor with ActorLogging{
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(){
    case TestException(msg) => Resume;
    case RestartException(msg) => Restart;
    case StopActorException(msg) => Stop;
  }

  override def receive: Receive = {
    case ServerStart => Init()
    case ShutdownServerMSG => context.system.terminate()
  }

  def Init(): Unit ={
    val onlineManager:ActorRef = context.actorOf(Props[OnlineUserManager], "onlineManager")
    val database:ActorRef = context.actorOf(Props[Database], "Database")

    //set up all service here:
    // Error Kernel Pattern

    val routeREST = new ChatREST(database)(context.system)

    val socketActorProps: Props = SocketActor.props(database, onlineManager)
    val routeSocket = new ChatSocket(socketActorProps)(context.system)


    // set REST Server
    val restBindingFuture = Http().newServerAt(host, restPort)
      .bind(routeREST.routeRest)
    restBindingFuture.onComplete{
      case Success(_) =>
        system.log.info(s"SERVER: REST Ready>>> REST at $host : $restPort")
      case Failure(ex) =>
        system.log.error("SERVER: REST Fail to bind, terminate system", ex)
        system.terminate()
    }(executionContext)


    // set Socket route
    val socketBindingFuture = Http().newServerAt(host, socketPort)
      .bindFlow(routeToFlow(routeSocket.routes))
    socketBindingFuture.onComplete{
      case Success(_) =>
        system.log.info(s"SERVER: Socket Ready>> SOCKET at $host : $socketPort")
      case Failure(ex) =>
        system.log.error("SERVER: Socket Fail to bind, terminate system", ex)
        system.terminate()
    }(executionContext)


    println("Press Enter to shutdown server")
    StdIn.readLine()
    context.system.terminate()

  }
}
