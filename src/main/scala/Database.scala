/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */


import akka.actor.Actor

import scala.collection.mutable


/**
 * Actor as in-memory database
 * -4 tables
 * -Account table for storing user account
 * -Friends table for storing each user's friend list
 * -Room table fro storing the room name of user created chatroom
 * -MessageBox: Store all messages [user-user, user-room]
 */
class Database extends Actor {

  var AccountTable = new mutable.HashMap[String, String]()

  var FriendTables = new mutable.HashMap[String, Set[String]]()

  var RoomTable: Set[String] = Set()

  var MessageBox: Array[Message] = Array[Message]()


  override def receive: Receive = {
    case Registration(a, b) => sender() ! addUser(a, b)
    case Login(a, b) => sender() ! checkUser(a, b)
    case a: AddFriend => sender() ! addFriend(a)
    case gfl: GetFriendList => sender() ! getFriends(gfl)
    case msg: Message => sendSingleMessage(msg)
    case history: GetSingleHistory => singleHistory(history)
    case msg: CreateRoom => createRoom(msg)
    case msg: RoomMessage => storeRoomMessage(msg)
    case GetRoomList => getRoomList()
    case msg: GetRoomHistory => getRoomHistory(msg)

    case _ =>
  }

  /**
   * Create user account, if username is unique
   * @param username
   * @param password
   * @return
   */
  def addUser(username: String, password: String): RegistrationState = {
    if (AccountTable.contains(username)) {
      RegistrationState(result = false, "Username Already Exist")
    } else {
      // create account record
      AccountTable.addOne((username, password))
      // create friend list
      FriendTables.addOne((username, Set()))
      RegistrationState(result = true, "Registration Success!")
    }
  }

  /**
   * Login user if username and password match record in account table
   * @param username
   * @param password
   * @return
   */
  def checkUser(username: String, password: String): LoginState = {
    if (AccountTable.contains(username)) {
      if (AccountTable(username).compareTo(password) == 0) {
        LoginState(result = true, info = "Login Success!")
      } else {
        LoginState(result = false, info = "Incorrect password, try again.")
      }
    } else {
      LoginState(result = false, "Username Not Exist")
    }
  }

  /**
   * Add friend to user's friend table
   *  -if the username exist and the username not equal to this user
   * @param msg
   * @return
   */
  def addFriend(msg: AddFriend): AddFriendState = {
    // check if friend exists
    FriendTables.foreach(com => {
//      println(s"user:>> ${com._1}")
      com._2.toArray
    })
    if (AccountTable.contains(msg.friendName)) {
      // check if add self
      if (msg.username != msg.friendName) {
        // check if friend already in table
        if (!FriendTables(msg.username).contains(msg.friendName)) {
          FriendTables(msg.username) = FriendTables(msg.username) + msg.friendName
          AddFriendState(result = true, msg.friendName, "Success Add Friend")
        } else {
          AddFriendState(result = false, msg.friendName, "Already become friends")
        }
      } else {
        AddFriendState(result = false, msg.friendName, "Cannot add self")
      }
    } else {
      // if not exist
      AddFriendState(result = false, msg.friendName, "User not exist")
    }
  }

  /**
   * store message to message box and report to socket actor
   * @param msg
   */
  def sendSingleMessage(msg: Message): Unit = {
    this.MessageBox = this.MessageBox.appended(msg)
    sender() ! msg
  }


  /**
   * Select all messages belong to two users' conversation
   * @param data
   */
  def singleHistory(data: GetSingleHistory): Unit = {
    var messages: Array[Message] = Array[Message]()
    MessageBox.foreach(msg => {
      val cond1: Boolean = (msg.sender == data.sender) || (msg.sender == data.receiver)
      val cond2: Boolean = (msg.receiver == data.sender) || (msg.receiver == data.receiver)
      if (cond1 && cond2) {
        messages = messages.appended(msg)
      }
    })
    sender() ! SingleHistoryReport(data.sender, messages)
  }

  /**
   * Select target user's friend table
   * @param msg
   * @return
   */
  def getFriends(msg: GetFriendList): FriendList = {
    val friends = FriendTables(msg.username).toArray
    FriendList(msg.username, friends)
  }


  /**
   * Store the room message but not report
   * @param msg
   */
  def storeRoomMessage(msg: RoomMessage): Unit = {
    this.MessageBox = this.MessageBox.appended(msg.msg)
  }

  /**
   * Select all rooms in room table
   */
  def getRoomList(): Unit = {
    val roomList: Array[Message] = RoomTable.map(r => Message("", r, "")).toArray
    sender() ! RoomListReport(result = true, "", roomList)
  }


  /**
   * Add new room record to room table
   * @param room
   */
  def createRoom(room: CreateRoom): Unit = {
    if (RoomTable.contains(room.room)) {
      val roomList: Array[Message] = RoomTable.map(r => Message("", r, "")).toArray
      sender() ! RoomListReport(result = false, "Room Already Exists", roomList)
    } else {
      RoomTable = RoomTable + room.room
      val roomList: Array[Message] = RoomTable.map(r => Message("", r, "")).toArray
      sender() ! RoomListReport(result = true, "Success create room", roomList)
    }
  }

  /**
   * Select all messages belongs to target room
   * @param history
   */
  def getRoomHistory(history: GetRoomHistory): Unit = {
    var messages: Array[Message] = Array[Message]()
    MessageBox.foreach(msg => {
      val cond: Boolean = (msg.receiver == history.room)
      if (cond) {
        messages = messages.appended(msg)
      }
    })
    sender() ! RoomHistory(history.room, messages)
  }

  /**
   * print messages when database restart
   * @param reason
   */
  override def postRestart(reason: Throwable): Unit = {
    context.system.log.info("db_restart")
  }

}
