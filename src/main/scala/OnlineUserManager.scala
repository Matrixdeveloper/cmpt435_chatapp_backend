/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */


import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

/**
 * Actor that responsible for managing online user interaction
 *  -register user state
 *  -record user-room state
 *  -redirect messages between users
 *  -redirect messages in room
 */

class OnlineUserManager extends Actor {
  // record online user
  var onlineUserTable = new mutable.HashMap[String, ActorRef]()
  // record user-room state: user can only stay in single room
  var userRoomTable = new mutable.HashMap[String, String]()
  // record room
  var roomTable = new mutable.HashMap[String, mutable.HashMap[String, ActorRef]]()

  override def receive: Receive = {
    case a: UserOnline => userOnline(a.username, a.ref)
    case a: UserOffline => userOffline(a.username)
    case a: SingleMessage => redirectMessageSingle(a)
    case a: EnterRoom => userEnterRoom(a)
    case a: RoomMessage => redirectMessageRoom(a)

  }

  /**
   * register login user state
   * @param username username
   * @param ref socket actor reference of this user
   */
  def userOnline(username: String, ref: ActorRef): Unit = {
    if (onlineUserTable.contains(username)) {
      onlineUserTable(username) = ref
      userRoomTable(username) = "current_no_room"
    } else {
      onlineUserTable.addOne((username, ref))
      userRoomTable(username) = "current_no_room"
    }
  }

  /**
   * clear offline user
   * @param username username
   */
  def userOffline(username: String): Unit = {
    if (onlineUserTable.contains(username)) {
      onlineUserTable.remove(username)
      val room = userRoomTable(username)
      if(room != "current_no_room") {
        roomTable(room).remove(username)
      }
      userRoomTable.remove(username)
    }
  }

  /**
   * change user-room state
   * @param msg, EnterRoom, username + room name
   */
  def userEnterRoom(msg: EnterRoom): Unit = {
    val user = msg.username
    val newRoom = msg.room
    if(userRoomTable(user) != "current_no_room"){
      val currentRoom = userRoomTable(user)
      roomTable(currentRoom).remove(user)
    }
    userRoomTable(user) = newRoom

    if(!roomTable.contains(newRoom)) {
      roomTable.addOne((newRoom, new mutable.HashMap[String, ActorRef]()))
    }
    roomTable(newRoom).addOne((user, onlineUserTable(user)))
  }

  /**
   * redirect user-user messages
   * @param msg, sender, receiver, content
   */
  def redirectMessageSingle(msg: SingleMessage): Unit = {
    //check if receiver online,if not, ignore
    if (onlineUserTable.contains(msg.msg.receiver)) {
      val receiverRef = onlineUserTable(msg.msg.receiver)
      receiverRef ! msg.msg
    }
  }

  /**
   * redirect user-room message
   * @param msg, sender, room name, content
   */
  def redirectMessageRoom(msg: RoomMessage): Unit = {
    val room = msg.msg.receiver

    if(roomTable.contains(room)){
      if(room != userRoomTable(msg.msg.sender)){
        println("user not in correct room")
      }else{
        roomTable(room).foreach(onlineUser=>{
          onlineUser._2 ! msg
        })
      }
    }else{
      println("room not exist")
    }

  }


}
