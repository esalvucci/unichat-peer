package user

import akka.actor.{Actor, Props}
import ui.MessageActor.{ErrorJoin, JoinedInChat}
import user.ChatRoom.JoinInChatRoom

private class ChatRoom extends Actor {
  private val chatRooms: List[String] = List("uni", "family", "friends")

  override def receive: Receive = {
    case JoinInChatRoom(username, chatRoom) =>
      //TODO contact the WhitePages
      if(chatRooms.contains(chatRoom)) {
        //TODO start failure detector
        //TODO create Router and pass it to UserInChat
        val actor = context.actorOf(UserInChat.props(username, List(), sender), "user-actor")
        sender ! JoinedInChat(username, chatRoom, actor)
      } else {
        sender ! ErrorJoin("Wrong data!")
      }
  }

}

object ChatRoom {
  def props: Props = Props(new ChatRoom)

  final case class JoinInChatRoom(username: String, chatRoom: String)
}
