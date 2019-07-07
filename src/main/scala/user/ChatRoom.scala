package user

import akka.actor.AbstractActor.Receive
import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing.BroadcastGroup
import ui.MessageActor.{ErrorJoin, JoinedInChat}
import user.ChatRoom.JoinInChatRoom
import user.UserInChat.Failure
import utility.FailureActor

private class ChatRoom extends Actor {
  private val chatRooms: List[String] = List("uni", "family", "friends")
  private var router: Option[ActorRef] = None
  private val failureActorName = "failure-actor"
  private implicit val senderActor: ActorRef = sender

  override def receive: Receive = {
    case JoinInChatRoom(username, chatRoom) =>
      //TODO contact the WhitePages
      if (chatRooms.contains(chatRoom)) {
        val paths = List(
          "akka.tcp://unichat-system@127.0.0.2:2554/user/messenger-actor/uni/frank",
          "akka.tcp://unichat-system@127.0.0.2:2553/user/messenger-actor/uni/enry"
        )
        val router = context.actorOf(BroadcastGroup(paths).props(), "router")

        val userInChatActor = context.actorOf(UserInChat.props(username, paths, router)(sender), name = username)

        val failureActor = context.actorOf(FailureActor.props(paths, userInChatActor, router), failureActorName)
        failureActor ! Terminated
        println(userInChatActor.path.toString)
        sender ! JoinedInChat(username, chatRoom, userInChatActor)
      } else {
        sender ! ErrorJoin("Wrong data!")
      }
    //TODO remove actor form matrix
  }

}

object ChatRoom {
  def props: Props = Props(new ChatRoom)

  final case class JoinInChatRoom(username: String, chatRoom: String)
}
