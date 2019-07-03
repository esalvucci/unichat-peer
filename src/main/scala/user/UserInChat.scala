package user

import akka.actor.{Actor, ActorRef, ActorSelection, Props}

private class UserInChat(username: String, usersInChat: List[ActorSelection], messenger: ActorRef) extends Actor {
  import UserInChat._

  override def receive: Receive = {
    // TODO send in broadcast router ! Broadcast(MessageBroadcast(content, username)))
    case MessageInChat(content) => println(s"will be sent in broadcast... $content")
  }

}

object UserInChat {
  def props(username: String, usersInChat: List[ActorSelection], messenger: ActorRef): Props =
    Props(new UserInChat(username, usersInChat, messenger))

  final case class MessageInChat(content: String)
}