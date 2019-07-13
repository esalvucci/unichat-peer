package server

import akka.actor.{Actor, ActorRef, Props}
import server.WhitePages.{DeleteUserFromChatRoom, ErrorUserNotInChatRoom, PutUserChatRoom, ReplyUsersInChat, _}


private class WhitePages extends Actor {

  private var chatRooms: Map[String, Seq[String]] = Map.empty

  override def receive: Receive = {
    case PutUserChatRoom(path: String, chatRoomName: String) =>
      val usersInChat: Seq[String] = chatRooms.getOrElse(chatRoomName, Seq.empty)
      sender() ! ReplyUsersInChat(chatRoomName, usersInChat)

      val updateUsersInChat: Seq[String] = usersInChat :+ path
      chatRooms = chatRooms + (chatRoomName -> updateUsersInChat)
      usersInChat.foreach(userPath =>
        context.actorSelection(userPath.substring(0, userPath.lastIndexOf("/"))) ! JoinedUserMessage(path))

    case DeleteUserFromChatRoom(path: String, chatRoomName: String) =>
      println("DeleteUserFromChatRoom")
      val usersInChat: Seq[String] = chatRooms.getOrElse(chatRoomName, Seq.empty)

      if (usersInChat.isEmpty) {
        sender() ! ErrorUserNotInChatRoom("The user is not a member of this chat: " + chatRoomName)
      } else {
        chatRooms = chatRooms + (chatRoomName -> usersInChat)
        sender() ! ReplyUserDeleted
        usersInChat.foreach(user => context.actorSelection(user) ! UnJoinedUserMessage(path))
      }
  }
}


object WhitePages {
  def props: Props = Props(new WhitePages)

  final case class JoinMe(userPath: String)
  final case class JoinedUserMessage(path: String)
  final case class PutUserChatRoom(path: String, chatRoomName: String)
  final case class DeleteUserFromChatRoom(path: String, chatRoomName: String)

  final case class UnJoinedUserMessage(path: String)
  final case class ReplyUsersInChat(chatRoomName: String, users: Seq[String])
  final case class ErrorUserNotInChatRoom(message: String)
  final object ReplyUserDeleted
}