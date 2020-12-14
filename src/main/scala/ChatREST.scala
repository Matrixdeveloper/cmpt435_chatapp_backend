/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, handleExceptions, handleRejections, path}
import akka.http.scaladsl.server.{Directive, ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.{cors, corsRejectionHandler}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.Future


/**
 * Rest server, provide service for login and registration
 * @param actor_db database actor
 * @param system implicit context
 */
class ChatREST(actor_db:ActorRef)(implicit val system:ActorSystem)
  extends ChatJsonProtocol with SprayJsonSupport{

  // for ask operation
  private implicit val timeout: Timeout = Timeout(1.seconds)

  // for cors setting
  val rejectionHandler: RejectionHandler =
    corsRejectionHandler.withFallback(RejectionHandler.default)
  val exceptionHandler: ExceptionHandler =
    ExceptionHandler {case e: NoSuchElementException =>
      complete(StatusCodes.NotFound -> e.getMessage)
    }
  val handleErrors: Directive[Unit] =
    handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

  /**
   * ask database to add new account
   * @return
   */
  def registry(request:Registration): Future[RegistrationState] = {
    actor_db.ask(request).mapTo[RegistrationState]
  }

  /**
   * ask database if the login data match the record
   * @return
   */
  def login(request:Login):Future[LoginState]={
    actor_db.ask(request).mapTo[LoginState]
  }

  val route0: Route = {
    handleErrors{
      cors(){
        handleErrors{
          concat(
            (path("registry")& post){
              entity(as[Registration]) { regData=>
                println(s"REST >>[registration] name-${regData.username} password-${regData.password}")
                onComplete(registry(regData)){ result=>
                  complete(200, result )
                }
              }
            },
            (path("login")& post){
              entity(as[Login]) { logData=>
                println(s"REST >>[login] name-${logData.username}")
                onComplete(login(logData)){ result=>
                  complete(200, result )
                }
              }
            }
          )
        }
      }
    }
  }

  val routeRest: Route = route0

}
