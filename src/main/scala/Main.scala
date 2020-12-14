/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */

import akka.actor.Props
import akka.actor.ActorSystem

/**
 * The main function of this server
 */
object Main extends App {
    val host = "localhost"
    val restPort = 8080
    val socketPort = 8081
    val name = "Default Server"

    val system = ActorSystem("ServerSystem")
    val supervisor = system.actorOf(Props(new Supervisor(host, restPort,socketPort,name)(system)),
      "ServerSupervisor")

    /**
     * tell supervisor start the server
     */
    supervisor ! ServerStart
}
