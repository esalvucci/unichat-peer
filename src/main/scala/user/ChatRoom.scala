package user

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing.{AddRoutee, BroadcastGroup, RemoveRoutee}
import ui.MessageActor.{ErrorJoin, JoinedInChat}
import user.ChatRoom.JoinInChatRoom
import utility.FailureActor

private class ChatRoom extends Actor {
  private val chatRooms: List[String] = List("uni", "family", "friends")
  private var router: Option[ActorRef] = None
  private val failureActorName = "failureActor"
  private implicit val senderActor: ActorRef = sender

  override def receive: Receive = {
    case JoinInChatRoom(username, chatRoom) =>
      //TODO contact the WhitePages
      if (chatRooms.contains(chatRoom)) {
        //TODO start failure detector
        val paths = List("akka.tcp://unichat-system@127.0.0.2:2552/user/messenger-actor/uni/frank")
        router = Some(context.actorOf(BroadcastGroup(paths).props(), "router"))

        val userInChatActor = context.actorOf(UserInChat.props(username, router.get), username)
        context watch userInChatActor

        val failureActor = context.actorOf(FailureActor.props(paths, userInChatActor), failureActorName)
        context watch failureActor

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
