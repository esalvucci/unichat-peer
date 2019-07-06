package user

import akka.actor.{Actor, Props, Terminated}
import akka.routing.BroadcastGroup
import ui.MessageActor.{ErrorJoin, JoinedInChat}
import user.ChatRoom.JoinInChatRoom

private class ChatRoom extends Actor {
  private val chatRooms: List[String] = List("uni", "family", "friends")

  override def receive: Receive = {
    case JoinInChatRoom(username, chatRoom) =>
      //TODO contact the WhitePages
      if (chatRooms.contains(chatRoom)) {
        //TODO start failure detector
        val paths = List(
          "akka.tcp://unichat-system@127.0.0.2:2554/user/messenger-actor/uni/frank"//,
          //"akka.tcp://unichat-system@127.0.0.2:2553/user/messenger-actor/uni/azzu"
        )
        val router = context.actorOf(BroadcastGroup(paths).props, "router")
        val actor = context.actorOf(UserInChat.props(username, paths.map(extractUsernameFrom), router, sender), name = username)
        println(actor.path.toString)
        sender ! JoinedInChat(username, chatRoom, actor)
      } else {
        sender ! ErrorJoin("Wrong data!")
      }
    case Terminated =>
    //TODO remove actor form matrix
  }

  private def extractUsernameFrom(user: String) = user.substring(user.lastIndexOf("/") + 1)

}

object ChatRoom {
  def props: Props = Props(new ChatRoom)

  final case class JoinInChatRoom(username: String, chatRoom: String)
}
