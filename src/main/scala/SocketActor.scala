/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */



import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.event.Logging
import spray.json._

object SocketActor {
  def props(database: ActorRef, onlineManager: ActorRef): Props =
    Props(new SocketActor(database, onlineManager))
}


/**
 * Socket actor
 * -For each login user, server will create an socket actor
 * -provide all services here
 * -add friend
 * -send message
 * -get user-user history
 * -create room
 * -enter room
 * -send message to room
 * -get user-room history
 *
 * @param database      database actor
 * @param onlineManager onlineUserMangerActor
 */
class SocketActor(database: ActorRef, onlineManager: ActorRef)
  extends Actor with Stash with ChatJsonProtocol {

  private val logger = Logging(context.system, this)
  private val client = context.parent
  private var username = "empty";

  override def receive: Receive = {
    // for room
    case msg: RoomMessage => reportRoomMsg(msg)
    case msg: RoomListReport => reportRoomList(msg)
    case msg: RoomHistory => reportRoomHistory(msg)
    // for single
    case msg: AddFriendState => reportFriend(msg)
    case msg: FriendList => reportFriendList(msg)
    case msg: Message => reportSingleMsg(msg)
    case msg: SingleHistoryReport => singleHistoryReport(msg)
    case msg: JsValue if msg.toString() == """{"method":"keepAlive"}""" => // ignore
    case msg: JsValue =>
//      println(msg)
      try {
        val tryConvert = msg.convertTo[ChatRequest]
        tryConvert.requestType match {
          case "InitSocket" => init(tryConvert)
          case "SendMsgSingle" => sendMsgSingleService(tryConvert)
          case "SendMsgRoom" => sendMsgRoomService(tryConvert)
          case "AddFriend" => addFriendService(tryConvert)
          case "CreateRoom" => createRoomService(tryConvert)
          case "GetSingleHistory" => getSingleHistory(tryConvert)
          case "GetRoomHistory" => getRoomHistory(tryConvert)
          case "GetFriendList" => getFriendList(tryConvert)
          case "GetRoomList" => getRoomList()
        }
      }
      catch {
        case e: Exception =>
          println(e)
          logger.error("Socket: Unexpected Message Type")
        // reply client with some notification
      }
    case _ => // ignore

  }

  /**
   * user login, register state in online user manager
   */
  def init(msg: ChatRequest): Unit = {
    this.username = msg.sender
    println(s"Socket start: >>$username")
    // tell online manager I come
    onlineManager ! UserOnline(this.username, self)
  }

  /**
   * interpret request to database
   * expect database send report back
   */
  def addFriendService(msg: ChatRequest): Unit = {
    val databaseRequest = AddFriend(msg.receiver, this.username)
    database ! databaseRequest
    //    println(s"actor: $self request to add friend")
  }

  /**
   * interpret database report and send back to user
   */
  def reportFriend(result: AddFriendState): Unit = {
//    println("user report get")
    val replyMessage =
      ChatRequest("AddFriendReport", this.username,
        result.friendName, result.result, result.info, Array()).toJson
    client ! replyMessage
  }

  /**
   * interpret user get friend request to database request
   * expect database send report
   */
  def getFriendList(request: ChatRequest): Unit = {
    val databaseRequest = GetFriendList(request.sender)
    database ! databaseRequest
  }

  /**
   * interpret friend report to json messsage
   */
  def reportFriendList(result: FriendList): Unit = {
    val list = result.friendList.map(s => Message("", s, ""))

    val replyMessage = ChatRequest(
      "FriendListReport", this.username, "", result = false, "", list
    ).toJson
    client ! replyMessage
  }

  /**
   * interpret user-user message request to database request
   * and tell online user manager try to redirect to possible online user
   * expect database send back report
   */
  def sendMsgSingleService(request: ChatRequest): Unit = {
    val databaseRequest = request.content(0)
    database ! databaseRequest
    onlineManager ! SingleMessage(databaseRequest)
  }

  /**
   * interpret user-user message report to json request
   * and sent it back to user
   */
  def reportSingleMsg(msg: Message): Unit = {
    val replyMessage = ChatRequest(
      "SingleMsgReport", msg.sender, msg.receiver, result = false, "", Array(msg)
    ).toJson
    client ! replyMessage
  }

  /**
   * interpret get user-user history to database request
   * expect database send back result
   */
  def getSingleHistory(request: ChatRequest): Unit = {
    val databaseRequest = GetSingleHistory(request.sender, request.receiver)
    database ! databaseRequest
  }

  /**
   * interpret user-user history to json message
   * sent back to user
   */
  def singleHistoryReport(result: SingleHistoryReport) = {
    val list = result.history
    val replyMessage = ChatRequest(
      "SingleHistoryReport", this.username, "", result = false, "", list
    ).toJson
    client ! replyMessage
  }


  /**
   * interpret create room request to database request
   * expect database send back report
   */
  def createRoomService(request: ChatRequest): Unit = {
    val databaseRequest = CreateRoom(request.sender, request.receiver)
    database ! databaseRequest
  }


  /**
   * interpret get room request to database request
   * expect database send back report
   */
  def getRoomList(): Unit = {
    database ! GetRoomList
  }


  /**
   * interpret room report to json message
   * send back to user
   */
  def reportRoomList(data: RoomListReport): Unit = {
    val replyMessage = ChatRequest(
      "RoomListReport", this.username, "", result = data.result, data.info, data.array
    ).toJson
    client ! replyMessage
  }


  /**
   * interpret send room message request to database request
   * let online user manager try to redirect the room message
   */
  def sendMsgRoomService(request: ChatRequest): Unit = {
    val msg = RoomMessage(request.content(0))
    database ! msg
    onlineManager ! msg
  }


  /**
   * interpret get room history request to database request
   * -also, tell online manager the user entered the room
   * expect database send back report
   */
  def getRoomHistory(request: ChatRequest): Unit = {
    val user = this.username
    val roomName = request.receiver
    val databaseRequest = GetRoomHistory(user, roomName)
    database ! databaseRequest
    onlineManager ! EnterRoom(user, roomName)
  }


  /**
   * interpret room history report to json message
   * send back to user
   */
  def reportRoomHistory(msg: RoomHistory): Unit = {
    val user = this.username
    val roomName = msg.room
    val list = msg.history
    val replyMessage = ChatRequest(
      "RoomHistoryReport", user, roomName, result = false, "", list
    ).toJson

    client ! replyMessage
  }


  /**
   * interpret room message  to json message
   * send back to user
   */
  def reportRoomMsg(msg: RoomMessage): Unit = {
    val user = this.username
    val roomName = msg.msg.receiver
    val list: Array[Message] = Array(msg.msg)
    val replyMessage = ChatRequest(
      "RoomMessageReport", user, roomName, result = false, "", list
    ).toJson

    client ! replyMessage
  }


  override def postStop(): Unit = {
    super.postStop()
    // tell online manager I leave
    onlineManager ! UserOffline(this.username)
  }

}
