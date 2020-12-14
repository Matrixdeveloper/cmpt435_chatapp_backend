/**
 * 2020 FALL CMPT435 Project
 * Name: Yue Weng
 * NSID: yuw857
 * Student number: 1121 9127
 */

import akka.actor.ActorRef

/**
 * Server internal message list
 */

trait ServerInternalMessage

case object ShutdownServerMSG extends ServerInternalMessage

case object ServerStart extends ServerInternalMessage

case class GetUserMsg(name:String) extends ServerInternalMessage


// for online manager
case class UserOnline(username:String, ref:ActorRef) extends ServerInternalMessage
case class UserOffline(username:String) extends ServerInternalMessage
case class EnterRoom(username:String, room:String) extends ServerInternalMessage


// for single service
case class AddFriend(friendName:String, username:String) extends ServerInternalMessage
case class AddFriendState(result: Boolean, friendName:String ,info:String) extends ServerInternalMessage
case class GetFriendList(username:String) extends ServerInternalMessage
case class FriendList(username:String, friendList:Array[String]) extends ServerInternalMessage
case class GetSingleHistory(sender:String, receiver:String) extends ServerInternalMessage
case class SingleHistoryReport(sender:String, history:Array[Message]) extends ServerInternalMessage
case class SingleMessage(msg:Message) extends ServerInternalMessage


// for room service
case object GetRoomList
case class CreateRoom(sender:String, room:String)
case class RoomListReport(result:Boolean,info:String ,array: Array[Message])
case class RoomMessage(msg:Message)
case class GetRoomHistory(sender:String, room:String)
case class RoomHistory(room:String, history:Array[Message])

