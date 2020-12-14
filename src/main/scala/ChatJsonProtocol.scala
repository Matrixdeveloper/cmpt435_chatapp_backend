/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */


import spray.json._

/**
 * This is the json protocol for the server
 * Akka.Spray Json will use the format defined here to interpret
 * the message
 */


// login
case class Registration(username:String, password:String)
case class RegistrationState(result:Boolean, info:String)
case class Login(username:String, password:String)
case class LoginState(result:Boolean, info:String)


// socket
case class Message(sender:String, receiver:String, text:String)
case class
ChatRequest(requestType:String,
            sender:String,
            receiver:String,
            result: Boolean ,
            notification: String,
            content:Array[Message])


trait ChatJsonProtocol extends DefaultJsonProtocol{
  implicit val registrationFormat:RootJsonFormat[Registration] = jsonFormat2(Registration)
  implicit val registrationResult:RootJsonFormat[RegistrationState] = jsonFormat2(RegistrationState)
  implicit val loginFormat:RootJsonFormat[Login] = jsonFormat2(Login)
  implicit val loginStateFormat:RootJsonFormat[LoginState] = jsonFormat2(LoginState)

  implicit val messageFormat:RootJsonFormat[Message] = jsonFormat3(Message)
  implicit val ChatRequestFormat:RootJsonFormat[ChatRequest] =
    jsonFormat6(ChatRequest)

}
